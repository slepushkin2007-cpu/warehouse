package com.example.warehouse.model;

import com.example.warehouse.model.Shipment.ShippedItem;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class Warehouse {

    private final Map<Product.ProductId, Product> products = new HashMap<>();
    private final Map<Product.ProductId, List<Batch>> batches = new LinkedHashMap<>();
    private final Map<String, StorageZone> zones = new HashMap<>();
    private final Map<String, Reservation> reservations = new LinkedHashMap<>();
    private final List<Shipment> shipments = new ArrayList<>();

    public void addProduct(Product product) {
        Objects.requireNonNull(product);
        if (products.putIfAbsent(product.id(), product) != null) {
            throw new WarehouseException(WarehouseException.Reason.INVALID_ARGUMENT,
                    "Товар уже зарегистрирован: " + product.id());
        }
        batches.putIfAbsent(product.id(), new ArrayList<>());
    }

    public void addZone(StorageZone zone) {
        Objects.requireNonNull(zone);
        zones.put(zone.name(), zone);
    }

    public List<Product> products() {
        return List.copyOf(products.values());
    }

    public List<StorageZone> zones() {
        return List.copyOf(zones.values());
    }

    public void receiveBatch(Product.ProductId productId, Batch batch, String zoneName) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(batch);
        Objects.requireNonNull(zoneName);

        Product product = products.get(productId);
        if (product == null) {
            throw new WarehouseException(WarehouseException.Reason.UNKNOWN_PRODUCT,
                    "Неизвестный товар: " + productId);
        }
        StorageZone zone = zones.get(zoneName);
        if (zone == null) {
            throw new WarehouseException(WarehouseException.Reason.UNKNOWN_ZONE,
                    "Неизвестная зона: " + zoneName);
        }
        if (product.category().mode() != zone.mode()) {
            throw new WarehouseException(WarehouseException.Reason.INCOMPATIBLE_TEMPERATURE,
                    "Категория " + product.category() + " требует " + product.category().mode()
                            + ", а зона " + zoneName + " имеет " + zone.mode());
        }
        batches.get(productId).add(batch);
    }

    public int stockOn(Product.ProductId productId, LocalDate date) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(date);
        return batches.getOrDefault(productId, List.of()).stream()
                .filter(b -> !b.isExpiredOn(date))
                .mapToInt(Batch::quantity)
                .sum();
    }

    public List<Batch> expiredOn(Product.ProductId productId, LocalDate date) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(date);
        return batches.getOrDefault(productId, List.of()).stream()
                .filter(b -> b.isExpiredOn(date))
                .toList();
    }

    public Map<Batch, Integer> expiringReport(LocalDate from, int days) {
        Objects.requireNonNull(from);
        if (days < 0) {
            throw new IllegalArgumentException("days >= 0");
        }
        Map<Batch, Integer> report = new LinkedHashMap<>();
        batches.values().stream()
                .flatMap(List::stream)
                .filter(b -> b.expiresWithin(from, days))
                .sorted(Comparator.comparing(Batch::expiryDate))
                .forEach(b -> report.put(b, b.quantity()));
        return Collections.unmodifiableMap(report);
    }

    public Shipment shipFefo(Product.ProductId productId, int quantity, LocalDate date) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(date);
        if (quantity <= 0) {
            throw new WarehouseException(WarehouseException.Reason.INVALID_ARGUMENT,
                    "Количество должно быть > 0");
        }
        Product product = products.get(productId);
        if (product == null) {
            throw new WarehouseException(WarehouseException.Reason.UNKNOWN_PRODUCT,
                    "Неизвестный товар: " + productId);
        }

        int available = stockOn(productId, date);
        if (available < quantity) {
            throw new WarehouseException(WarehouseException.Reason.INSUFFICIENT_STOCK,
                    "Недостаточно остатка: доступно " + available + ", запрошено " + quantity);
        }

        List<Batch> fresh = batches.get(productId).stream()
                .filter(b -> !b.isExpiredOn(date))
                .sorted(Comparator.comparing(Batch::expiryDate))
                .toList();

        List<ShippedItem> shipped = new ArrayList<>();
        int remaining = quantity;
        for (Batch b : fresh) {
            if (remaining == 0) break;
            int take = Math.min(b.quantity(), remaining);
            if (take == 0) continue;
            replaceBatch(productId, b, b.withQuantity(b.quantity() - take));
            shipped.add(new ShippedItem(b.number(), take));
            remaining -= take;
        }

        Shipment shipment = new Shipment.Fefo(date, shipped);
        shipments.add(shipment);
        return shipment;
    }

    public void ensureNotExpired(Product.ProductId productId, int quantity, LocalDate date) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(date);
        int fresh = stockOn(productId, date);
        if (fresh == 0) {
            throw new WarehouseException(WarehouseException.Reason.EXPIRED_BATCH,
                    "Нет неистёкших партий товара " + productId + " на " + date);
        }
        if (fresh < quantity) {
            throw new WarehouseException(WarehouseException.Reason.INSUFFICIENT_STOCK,
                    "Недостаточно неистёкшего остатка: " + fresh + " < " + quantity);
        }
    }

    public List<Shipment> shipments() {
        return List.copyOf(shipments);
    }

    public Reservation reserve(String orderId, Product.ProductId productId,
                               int quantity, long ttlSeconds, Instant now) {
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(productId);
        Objects.requireNonNull(now);

        Product product = products.get(productId);
        if (product == null) {
            throw new WarehouseException(WarehouseException.Reason.UNKNOWN_PRODUCT,
                    "Неизвестный товар: " + productId);
        }
        if (quantity <= 0) {
            throw new WarehouseException(WarehouseException.Reason.INVALID_ARGUMENT,
                    "Количество должно быть > 0");
        }

        LocalDate today = LocalDate.ofInstant(now, ZoneId.systemDefault());
        int reservedNow = activeReservedQuantity(productId, now);
        int available = batches.get(productId).stream()
                .filter(b -> !b.isExpiredOn(today))
                .mapToInt(Batch::quantity).sum() - reservedNow;

        if (available < quantity) {
            throw new WarehouseException(WarehouseException.Reason.INSUFFICIENT_STOCK,
                    "Недостаточно свободного остатка: " + available + " < " + quantity);
        }

        Reservation r = new Reservation(orderId, productId, "*", quantity, ttlSeconds);
        reservations.put(orderId, r);
        return r;
    }

    public void cancelExpiredReservation(String orderId, Instant now) {
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(now);
        Reservation r = reservations.get(orderId);
        if (r == null) {
            throw new WarehouseException(WarehouseException.Reason.RESERVATION_NOT_FOUND,
                    "Резерв не найден: " + orderId);
        }
        if (!r.isExpiredAt(now)) {
            throw new WarehouseException(WarehouseException.Reason.RESERVATION_EXPIRED,
                    "Резерв ещё активен: " + orderId);
        }
        r.cancel();
        reservations.remove(orderId);
    }

    public List<Reservation> activeReservations() {
        return reservations.values().stream().filter(r -> !r.isCancelled()).toList();
    }

    private int activeReservedQuantity(Product.ProductId productId, Instant now) {
        return reservations.values().stream()
                .filter(r -> !r.isCancelled() && !r.isExpiredAt(now))
                .filter(r -> r.productId().equals(productId))
                .mapToInt(Reservation::quantity)
                .sum();
    }

    public boolean isCompatible(Product.ProductId productId, String zoneName) {
        Product p = products.get(productId);
        StorageZone z = zones.get(zoneName);
        if (p == null) {
            throw new WarehouseException(WarehouseException.Reason.UNKNOWN_PRODUCT,
                    "Неизвестный товар: " + productId);
        }
        if (z == null) {
            throw new WarehouseException(WarehouseException.Reason.UNKNOWN_ZONE,
                    "Неизвестная зона: " + zoneName);
        }
        return p.category().mode() == z.mode();
    }


    private void replaceBatch(Product.ProductId productId, Batch oldBatch, Batch newBatch) {
        List<Batch> list = batches.get(productId);
        int idx = list.indexOf(oldBatch);
        if (idx < 0) {
            throw new IllegalStateException("Batch vanished: " + oldBatch);
        }
        list.set(idx, newBatch);
    }

    public Set<String> batchNumbers(Product.ProductId productId) {
        return batches.getOrDefault(productId, List.of()).stream()
                .map(Batch::number)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public Optional<Batch> findBatch(Product.ProductId productId, String number) {
        return batches.getOrDefault(productId, List.of()).stream()
                .filter(b -> b.number().equals(number))
                .findFirst();
    }
}