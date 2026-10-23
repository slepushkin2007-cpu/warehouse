package com.example.warehouse.app;

import com.example.warehouse.model.Batch;
import com.example.warehouse.model.Category;
import com.example.warehouse.model.Product;
import com.example.warehouse.model.Reservation;
import com.example.warehouse.model.Shipment;
import com.example.warehouse.model.StorageZone;
import com.example.warehouse.model.TemperatureMode;
import com.example.warehouse.model.Warehouse;
import com.example.warehouse.model.WarehouseException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class Demo {

    private Demo() {
    }

    public static void main(String[] args) {
        Warehouse wh = new Warehouse();
        wh.addZone(new StorageZone("FR-1", TemperatureMode.FROZEN));
        wh.addZone(new StorageZone("CH-1", TemperatureMode.CHILLED));
        wh.addZone(new StorageZone("AM-1", TemperatureMode.AMBIENT));
        wh.addZone(new StorageZone("AM-2", TemperatureMode.AMBIENT));

        List<Product> products = new ArrayList<>();

        products.add(new Product(new Product.ProductId("SKU-001"), "Треска", Category.FROZEN_FISH));
        products.add(new Product(new Product.ProductId("SKU-002"), "Сельдь", Category.FROZEN_FISH));
        products.add(new Product(new Product.ProductId("SKU-003"), "Лосось", Category.FROZEN_FISH));
        products.add(new Product(new Product.ProductId("SKU-004"), "Минтай", Category.FROZEN_FISH));
        products.add(new Product(new Product.ProductId("SKU-005"), "Скумбрия", Category.FROZEN_FISH));

        products.add(new Product(new Product.ProductId("SKU-006"), "Пломбир", Category.ICE_CREAM));
        products.add(new Product(new Product.ProductId("SKU-007"), "Эскимо", Category.ICE_CREAM));
        products.add(new Product(new Product.ProductId("SKU-008"), "Рожок", Category.ICE_CREAM));
        products.add(new Product(new Product.ProductId("SKU-009"), "Сорбет", Category.ICE_CREAM));
        products.add(new Product(new Product.ProductId("SKU-010"), "Фруктовый лед", Category.ICE_CREAM));

        products.add(new Product(new Product.ProductId("SKU-011"), "Молоко 3.2%", Category.MILK));
        products.add(new Product(new Product.ProductId("SKU-012"), "Молоко 1.5%", Category.MILK));
        products.add(new Product(new Product.ProductId("SKU-013"), "Топленое молоко", Category.MILK));
        products.add(new Product(new Product.ProductId("SKU-014"), "Козье молоко", Category.MILK));
        products.add(new Product(new Product.ProductId("SKU-015"), "Соевое молоко", Category.MILK));

        products.add(new Product(new Product.ProductId("SKU-016"), "Йогурт клубничный", Category.YOGURT));
        products.add(new Product(new Product.ProductId("SKU-017"), "Йогурт персиковый", Category.YOGURT));
        products.add(new Product(new Product.ProductId("SKU-018"), "Йогурт натуральный", Category.YOGURT));
        products.add(new Product(new Product.ProductId("SKU-019"), "Греческий йогурт", Category.YOGURT));
        products.add(new Product(new Product.ProductId("SKU-020"), "Йогурт черничный", Category.YOGURT));

        products.add(new Product(new Product.ProductId("SKU-021"), "Гречка", Category.CEREALS));
        products.add(new Product(new Product.ProductId("SKU-022"), "Рис", Category.CEREALS));
        products.add(new Product(new Product.ProductId("SKU-023"), "Пшено", Category.CEREALS));
        products.add(new Product(new Product.ProductId("SKU-024"), "Овсянка", Category.CEREALS));
        products.add(new Product(new Product.ProductId("SKU-025"), "Перловка", Category.CEREALS));

        products.add(new Product(new Product.ProductId("SKU-026"), "Тушёнка говяжья", Category.CANNED_FOOD));
        products.add(new Product(new Product.ProductId("SKU-027"), "Тушёнка свиная", Category.CANNED_FOOD));
        products.add(new Product(new Product.ProductId("SKU-028"), "Сгущёнка", Category.CANNED_FOOD));
        products.add(new Product(new Product.ProductId("SKU-029"), "Шпроты", Category.CANNED_FOOD));
        products.add(new Product(new Product.ProductId("SKU-030"), "Кукуруза", Category.CANNED_FOOD));

        products.forEach(wh::addProduct);
        LocalDate today = LocalDate.now();

        int batchCounter = 1;
        for (Product p : products) {
            wh.receiveBatch(p.id(), new Batch("B-%03d".formatted(batchCounter++), today.minusDays(40), today.minusDays(2), 10), getZoneFor(p));
            wh.receiveBatch(p.id(), new Batch("B-%03d".formatted(batchCounter++), today.minusDays(5), today.plusDays(3), 25), getZoneFor(p));
            wh.receiveBatch(p.id(), new Batch("B-%03d".formatted(batchCounter++), today, today.plusDays(p.category().defaultShelfLifeDays()), 40), getZoneFor(p));
        }

        wh.shipFefo(products.get(0).id(), 30, today);
        wh.shipFefo(products.get(10).id(), 10, today);
        wh.shipFefo(products.get(20).id(), 50, today);
        wh.shipFefo(products.get(4).id(), 65, today);

        try { wh.shipFefo(products.get(0).id(), 1000, today); } catch (WarehouseException ignored) {}
        try { wh.shipFefo(products.get(4).id(), 100, today); } catch (WarehouseException ignored) {}

        try {
            wh.reserve("ORD-100", products.get(1).id(), 15, 3600, Instant.now());
            wh.reserve("ORD-101", products.get(11).id(), 20, 3600, Instant.now());
            wh.reserve("ORD-102", products.get(21).id(), 10, 3600, Instant.now());
            wh.reserve("ORD-103", products.get(5).id(), 5, 3600, Instant.now());
        } catch (WarehouseException ignored) {}

        System.out.println("================================================================================");
        System.out.println("                    ПОЛНЫЙ ОТЧЕТ ПО СКЛАДУ НА " + today);
        System.out.println("================================================================================");
        System.out.printf("Всего товаров: %d | Всего зон: %d | Всего отгрузок: %d | Активных резервов: %d%n",
                wh.products().size(), wh.zones().size(), wh.shipments().size(), wh.activeReservations().size());
        System.out.println("--------------------------------------------------------------------------------");

        for (Product p : wh.products()) {
            System.out.println("\n[ТОВАР]: " + p.id().sku() + " - " + p.name());
            System.out.println("  Категория: " + p.category().describe());
            System.out.println("  Требуемый режим: " + p.category().mode());

            int totalStock = wh.stockOn(p.id(), today);
            int expiredCount = wh.expiredOn(p.id(), today).size();
            System.out.printf("  ОБЩИЙ ОСТАТОК (неистекший): %d шт. | Просроченных партий: %d%n", totalStock, expiredCount);

            long reservedQty = wh.activeReservations().stream()
                    .filter(r -> r.productId().equals(p.id()))
                    .mapToInt(Reservation::quantity)
                    .sum();
            if (reservedQty > 0) {
                System.out.println("  В РЕЗЕРВЕ: " + reservedQty + " шт.");
            }

            var numbers = wh.batchNumbers(p.id());
            System.out.println("  ПАРТИИ:");
            if (numbers.isEmpty()) {
                System.out.println("    Нет партий на складе.");
            } else {
                for (String num : numbers) {
                    wh.findBatch(p.id(), num).ifPresent(b -> {
                        boolean isExpired = b.isExpiredOn(today);
                        System.out.printf("    - %s | Кол-во: %3d | Пр-во: %s | Срок: %s %s%n",
                                b.number(), b.quantity(), b.productionDate(), b.expiryDate(),
                                isExpired ? "[ПРОСРОЧЕНА]" : "[ОК]");
                    });
                }
            }

            System.out.println("  ИСТОРИЯ ОТГРУЗОК:");
            boolean hasShipments = false;
            for (Shipment s : wh.shipments()) {
                for (Shipment.ShippedItem item : s.items()) {
                    if (numbers.contains(item.batchNumber())) {
                        System.out.printf("    - %s | Партия: %s | Кол-во: %d%n", s.date(), item.batchNumber(), item.quantity());
                        hasShipments = true;
                    }
                }
            }
            if (!hasShipments) {
                System.out.println("    Отгрузок не было.");
            }
        }

        System.out.println("\n================================================================================");

        System.out.println("\n\n=== ЛОГ ВЫПОЛНЕННЫХ ОПЕРАЦИЙ ===");

        System.out.println("\n--- FEFO-ОТГРУЗКИ (списывается ближайший срок) ---");
        System.out.println("Отгрузка " + products.get(0).name() + " (30 шт.) - УСПЕШНО");
        System.out.println("Отгрузка " + products.get(10).name() + " (10 шт.) - УСПЕШНО");
        System.out.println("Отгрузка " + products.get(20).name() + " (50 шт.) - УСПЕШНО");
        System.out.println("Отгрузка " + products.get(4).name() + " (65 шт.) - УСПЕШНО");

        System.out.println("\n--- ОШИБКИ ОТГРУЗКИ (превышение остатка) ---");
        System.out.println("Попытка отгрузить 1000 шт. " + products.get(0).name() + ":");
        try {
            wh.shipFefo(products.get(0).id(), 1000, today);
        } catch (WarehouseException e) {
            System.out.println("  [ОШИБКА] " + e.reason() + ": " + e.getMessage());
        }

        System.out.println("Попытка отгрузить 100 шт. " + products.get(4).name() + " (остаток исчерпан):");
        try {
            wh.shipFefo(products.get(4).id(), 100, today);
        } catch (WarehouseException e) {
            System.out.println("  [ОШИБКА] " + e.reason() + ": " + e.getMessage());
        }

        System.out.println("\n--- РЕЗЕРВИРОВАНИЕ (TTL 1 час) ---");
        try {
            wh.reserve("ORD-100", products.get(1).id(), 15, 3600, Instant.now());
            System.out.println("  Зарезервировано 15 шт. " + products.get(1).name());

            wh.reserve("ORD-101", products.get(11).id(), 20, 3600, Instant.now());
            System.out.println("  Зарезервировано 20 шт. " + products.get(11).name());

            wh.reserve("ORD-102", products.get(21).id(), 10, 3600, Instant.now());
            System.out.println("  Зарезервировано 10 шт. " + products.get(21).name());

            wh.reserve("ORD-103", products.get(5).id(), 5, 3600, Instant.now());
            System.out.println("  Зарезервировано 5 шт. " + products.get(5).name());

            System.out.println("Попытка зарезервировать сверх остатка:");
            wh.reserve("ORD-ERR", products.get(1).id(), 500, 3600, Instant.now());
        } catch (WarehouseException e) {
            System.out.println("  [ОШИБКА] " + e.reason() + ": " + e.getMessage());
        }

        System.out.println("\n--- ПРОВЕРКА ТЕМПЕРАТУРНЫХ РЕЖИМОВ ---");
        System.out.println("  " + products.get(2).name() + " (FROZEN) + AM-1 (AMBIENT): " + wh.isCompatible(products.get(2).id(), "AM-1"));
        System.out.println("  " + products.get(12).name() + " (CHILLED) + FR-1 (FROZEN): " + wh.isCompatible(products.get(12).id(), "FR-1"));
        System.out.println("  " + products.get(22).name() + " (AMBIENT) + AM-2 (AMBIENT): " + wh.isCompatible(products.get(22).id(), "AM-2"));
        System.out.println("  " + products.get(6).name() + " (FROZEN) + FR-1 (FROZEN): " + wh.isCompatible(products.get(6).id(), "FR-1"));

        System.out.println("Попытка принять " + products.get(2).name() + " в AM-1:");
        try {
            wh.receiveBatch(products.get(2).id(), new Batch("ERR-1", today, today.plusDays(1), 1), "AM-1");
        } catch (WarehouseException e) {
            System.out.println("  [ОШИБКА] " + e.reason() + ": " + e.getMessage());
        }

        System.out.println("Попытка принять " + products.get(12).name() + " в FR-1:");
        try {
            wh.receiveBatch(products.get(12).id(), new Batch("ERR-2", today, today.plusDays(1), 1), "FR-1");
        } catch (WarehouseException e) {
            System.out.println("  [ОШИБКА] " + e.reason() + ": " + e.getMessage());
        }

        System.out.println("\n================================================================================");
    }

    private static String getZoneFor(Product p) {
        return switch (p.category().mode()) {
            case FROZEN  -> "FR-1";
            case CHILLED -> "CH-1";
            case AMBIENT -> "AM-1";
        };
    }
}