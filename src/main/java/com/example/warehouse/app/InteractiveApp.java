package com.example.warehouse.app;

import com.example.warehouse.model.Batch;
import com.example.warehouse.model.Category;
import com.example.warehouse.model.Product;
import com.example.warehouse.model.Reservation;
import com.example.warehouse.model.StorageZone;
import com.example.warehouse.model.TemperatureMode;
import com.example.warehouse.model.Warehouse;
import com.example.warehouse.model.WarehouseException;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Scanner;

public final class InteractiveApp {

    private static final Warehouse wh = new Warehouse();
    private static final Scanner scanner = new Scanner(System.in);
    private static final LocalDate today = LocalDate.now();

    private InteractiveApp() {
    }

    public static void main(String[] args) {
        wh.addZone(new StorageZone("CH-1", TemperatureMode.CHILLED));
        wh.addZone(new StorageZone("FR-1", TemperatureMode.FROZEN));
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

        int batchCounter = 1;
        for (Product p : products) {
            String zone = switch (p.category().mode()) {
                case FROZEN -> "FR-1";
                case CHILLED -> "CH-1";
                case AMBIENT -> "AM-1";
            };
            wh.receiveBatch(p.id(), new Batch("B-%03d".formatted(batchCounter++), today.minusDays(40), today.minusDays(2), 10), zone);
            wh.receiveBatch(p.id(), new Batch("B-%03d".formatted(batchCounter++), today.minusDays(5), today.plusDays(3), 25), zone);
            wh.receiveBatch(p.id(), new Batch("B-%03d".formatted(batchCounter++), today, today.plusDays(p.category().defaultShelfLifeDays()), 40), zone);
        }

        System.out.println("=== Добро пожаловать на интерактивный склад! ===");
        System.out.println("(Сегодня: " + today + ")");

        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> showStock();
                    case "2" -> createReservation();
                    case "3" -> cancelReservation();
                    case "4" -> showActiveReservations();
                    case "5" -> expiringReport();
                    case "6" -> checkCompatibility();
                    case "7" -> testTimeoutCancellation();
                    case "0" -> {
                        System.out.println("Выход. До свидания!");
                        return;
                    }
                    default -> System.out.println("Неизвестная команда. Попробуйте снова.");
                }
            } catch (WarehouseException e) {
                System.out.println("\n[ОШИБКА СКЛАДА]: " + e.reason() + " - " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("\n[ОШИБКА ВВОДА]: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\n[ОШИБКА]: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private static void printMenu() {
        System.out.println("---------------------------------");
        System.out.println("1. Показать остатки на складе");
        System.out.println("2. Зарезервировать товар (Создать резерв)");
        System.out.println("3. Отменить резерв по таймауту (TTL)");
        System.out.println("4. Показать активные резервы");
        System.out.println("5. Отчет 'Сгорит в ближайшие N дней'");
        System.out.println("6. Проверить совместимость (Товар и Зона)");
        System.out.println("7. Симуляция истечения TTL и автоотмены");
        System.out.println("0. Выход");
        System.out.print("Выберите действие: ");
    }

    private static void showStock() {
        System.out.println("Текущие остатки:");
        for (Product p : wh.products()) {
            int stock = wh.stockOn(p.id(), today);
            System.out.printf("  %s (%s) - %d шт.%n", p.name(), p.id().sku(), stock);
        }
    }

    private static void createReservation() {
        showStock();
        System.out.print("Введите SKU товара: ");
        String sku = scanner.nextLine().trim();
        System.out.print("Введите ID заказа (например, ORD-5): ");
        String orderId = scanner.nextLine().trim();
        System.out.print("Введите количество: ");
        int qty = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Введите время жизни резерва в секундах (TTL): ");
        long ttl = Long.parseLong(scanner.nextLine().trim());

        Reservation r = wh.reserve(orderId, new Product.ProductId(sku), qty, ttl, Instant.now());
        System.out.println("УСПЕХ: Резерв " + r.orderId() + " на " + r.quantity() + " шт. успешно создан.");
    }

    private static void cancelReservation() {
        System.out.print("Введите ID заказа для отмены: ");
        String orderId = scanner.nextLine().trim();

        System.out.print("На сколько секунд перемотать время вперед для проверки TTL? ");
        long secondsForward = Long.parseLong(scanner.nextLine().trim());

        wh.cancelExpiredReservation(orderId, Instant.now().plusSeconds(secondsForward));
        System.out.println("УСПЕХ: Резерв " + orderId + " отменен (сгорел по таймауту).");
    }

    private static void showActiveReservations() {
        var active = wh.activeReservations();
        if (active.isEmpty()) {
            System.out.println("Нет активных резервов.");
        } else {
            System.out.println("Активные резервы:");
            for (Reservation r : active) {
                System.out.printf("  Заказ: %s | Товар: %s | Кол-во: %d | Истекает: %s%n",
                        r.orderId(), r.productId().sku(), r.quantity(), r.expiresAt());
            }
        }
    }

    private static void expiringReport() {
        System.out.print("Введите количество дней (N) для отчета: ");
        int days = Integer.parseInt(scanner.nextLine().trim());

        var report = wh.expiringReport(today, days);
        if (report.isEmpty()) {
            System.out.println("Нет партий, истекающих в ближайшие " + days + " дн.");
        } else {
            System.out.println("Отчет 'Сгорит в ближайшие " + days + " дн.':");
            System.out.println("  (Партия - Количество - Сумма потерь в шт.)");
            report.forEach((b, q) -> System.out.printf("  %s - %d шт. (срок: %s)%n", b.number(), q, b.expiryDate()));
        }
    }

    private static void checkCompatibility() {
        System.out.println("Доступные товары:");
        for (Product p : wh.products()) {
            System.out.printf("  %s - %s (Режим: %s)%n", p.id().sku(), p.name(), p.category().mode());
        }
        System.out.print("Введите SKU товара: ");
        String sku = scanner.nextLine().trim();

        System.out.println("Доступные зоны:");
        for (StorageZone z : wh.zones()) {
            System.out.printf("  %s (Режим: %s)%n", z.name(), z.mode());
        }
        System.out.print("Введите название зоны: ");
        String zone = scanner.nextLine().trim().toUpperCase();

        boolean compatible = wh.isCompatible(new Product.ProductId(sku), zone);
        System.out.println("\nРезультат: " + (compatible ? "СОВМЕСТИМО (Можно хранить)" : "НЕСОВМЕСТИМО (Испортится!)"));
    }

    private static void testTimeoutCancellation() {
        System.out.println("--- Тест автоотмены TTL ---");
        try {
            System.out.println("Создаем резерв ORD-TEST на 5 секунд...");
            wh.reserve("ORD-TEST", new Product.ProductId("SKU-001"), 5, 5, Instant.now());

            System.out.println("Резерв создан. Ждем 6 секунд...");
            Thread.sleep(6000);

            System.out.println("Пытаемся отменить (должно сработать, т.к. время вышло)...");
            wh.cancelExpiredReservation("ORD-TEST", Instant.now());
            System.out.println("УСПЕХ: Система сама позволила отменить просроченный резерв.");

        } catch (WarehouseException e) {
            System.out.println("[ОШИБКА] " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}