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
import java.util.List;

public final class Demo {

    private Demo() {
    }

    public static void main(String[] args) {
        Warehouse wh = new Warehouse();

        StorageZone frozen  = new StorageZone("FR-1", TemperatureMode.FROZEN);
        StorageZone chilled = new StorageZone("CH-1", TemperatureMode.CHILLED);
        StorageZone ambient = new StorageZone("AM-1", TemperatureMode.AMBIENT);
        StorageZone wrong   = new StorageZone("AM-2", TemperatureMode.AMBIENT);
        wh.addZone(frozen);
        wh.addZone(chilled);
        wh.addZone(ambient);
        wh.addZone(wrong);

        List<Product> products = List.of(
                new Product(new Product.ProductId("SKU-001"), "Треска",    Category.FROZEN_FISH),
                new Product(new Product.ProductId("SKU-002"), "Мороженое", Category.ICE_CREAM),
                new Product(new Product.ProductId("SKU-003"), "Молоко",    Category.MILK),
                new Product(new Product.ProductId("SKU-004"), "Йогурт",    Category.YOGURT),
                new Product(new Product.ProductId("SKU-005"), "Крупа",     Category.CEREALS),
                new Product(new Product.ProductId("SKU-006"), "Тушёнка",   Category.CANNED_FOOD),
                new Product(new Product.ProductId("SKU-007"), "Сельдь",    Category.FROZEN_FISH),
                new Product(new Product.ProductId("SKU-008"), "Кефир",     Category.MILK),
                new Product(new Product.ProductId("SKU-009"), "Сгущёнка",  Category.CANNED_FOOD),
                new Product(new Product.ProductId("SKU-010"), "Гречка",    Category.CEREALS)
        );
        products.forEach(wh::addProduct);

        LocalDate today = LocalDate.now();

        wh.receiveBatch(products.get(0).id(),
                new Batch("B-001", today.minusDays(30), today.plusDays(5),   10), "FR-1");
        wh.receiveBatch(products.get(0).id(),
                new Batch("B-002", today.minusDays(20), today.plusDays(20),  15), "FR-1");
        wh.receiveBatch(products.get(1).id(),
                new Batch("B-003", today.minusDays(60), today.plusDays(300), 50), "FR-1");
        wh.receiveBatch(products.get(2).id(),
                new Batch("B-004", today.minusDays(2),  today.plusDays(2),   8),  "CH-1");
        wh.receiveBatch(products.get(2).id(),
                new Batch("B-005", today.minusDays(5),  today.minusDays(1),  5),  "CH-1");
        wh.receiveBatch(products.get(3).id(),
                new Batch("B-006", today.minusDays(3),  today.plusDays(7),   20), "CH-1");
        wh.receiveBatch(products.get(4).id(),
                new Batch("B-007", today.minusDays(10), today.plusDays(700), 100),"AM-1");
        wh.receiveBatch(products.get(5).id(),
                new Batch("B-008", today.minusDays(200),today.plusDays(900), 200),"AM-1");
        wh.receiveBatch(products.get(6).id(),
                new Batch("B-009", today.minusDays(15), today.plusDays(15),  12), "FR-1");
        wh.receiveBatch(products.get(7).id(),
                new Batch("B-010", today.minusDays(1),  today.plusDays(6),   30), "CH-1");
        wh.receiveBatch(products.get(8).id(),
                new Batch("B-011", today.minusDays(100),today.plusDays(1000),40), "AM-1");
        wh.receiveBatch(products.get(9).id(),
                new Batch("B-012", today.minusDays(5),  today.plusDays(715), 60), "AM-1");

        for (int i = 13; i <= 25; i++) {
            Product p = products.get(i % products.size());
            String zone = switch (p.category().mode()) {
                case FROZEN  -> "FR-1";
                case CHILLED -> "CH-1";
                case AMBIENT -> "AM-1";
            };
            wh.receiveBatch(p.id(),
                    new Batch("B-%03d".formatted(i),
                            today.minusDays(i),
                            today.plusDays(i * 3),
                            5 + i),
                    zone);
        }

        System.out.println("=== Товары: " + wh.products().size() + " ===");
        wh.products().forEach(p -> System.out.printf("  %s | %s | %s%n",
                p.id().sku(), p.name(), p.category().describe()));

        System.out.println();
        System.out.println("=== Партии SKU-001 ===");
        System.out.println("  batchNumbers = " + wh.batchNumbers(products.get(0).id()));

        System.out.println();
        System.out.println("=== Остаток SKU-001 на " + today + " ===");
        System.out.println("  " + wh.stockOn(products.get(0).id(), today));

        System.out.println();
        System.out.println("=== Просроченные SKU-003 на " + today + " ===");
        wh.expiredOn(products.get(2).id(), today).forEach(b -> System.out.println("  " + b));

        System.out.println();
        System.out.println("=== FEFO-отгрузка SKU-001 x 12 ===");
        Shipment sh = wh.shipFefo(products.get(0).id(), 12, today);
        System.out.println("  " + sh.items() + " всего=" + sh.totalQuantity());

        System.out.println();
        System.out.println("=== Отгрузка сверх остатка (ожидаем исключение) ===");
        try {
            wh.shipFefo(products.get(0).id(), 10_000, today);
        } catch (WarehouseException e) {
            System.out.println("  " + e.reason() + ": " + e.getMessage());
        }

        System.out.println();
        System.out.println("=== Отчёт 'сгорит в ближайшие 7 дней' ===");
        wh.expiringReport(today, 7)
                .forEach((b, q) -> System.out.printf("  %s до %s — %d шт.%n",
                        b.number(), b.expiryDate(), q));

        System.out.println();
        System.out.println("=== Резерв ===");
        Reservation r = wh.reserve("ORD-1", products.get(1).id(), 10, 1, Instant.now());
        System.out.println("  создан: " + r.orderId() + " qty=" + r.quantity());

        System.out.println();
        System.out.println("=== Совместимость ===");
        System.out.println("  Молоко + CH-1: " + wh.isCompatible(products.get(2).id(), "CH-1"));
        System.out.println("  Молоко + AM-1: " + wh.isCompatible(products.get(2).id(), "AM-1"));

        System.out.println();
        System.out.println("=== Несовместимый приём (ожидаем исключение) ===");
        try {
            wh.receiveBatch(products.get(2).id(),
                    new Batch("B-ERR", today, today.plusDays(1), 1), "AM-1");
        } catch (WarehouseException e) {
            System.out.println("  " + e.reason() + ": " + e.getMessage());
        }

        System.out.println();
        System.out.println("Всего отгрузок: " + wh.shipments().size());
    }
}