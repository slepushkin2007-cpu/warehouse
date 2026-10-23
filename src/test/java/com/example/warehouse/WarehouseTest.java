package com.example.warehouse;

import com.example.warehouse.model.Batch;
import com.example.warehouse.model.Category;
import com.example.warehouse.model.Product;
import com.example.warehouse.model.Reservation;
import com.example.warehouse.model.Shipment;
import com.example.warehouse.model.StorageZone;
import com.example.warehouse.model.TemperatureMode;
import com.example.warehouse.model.Warehouse;
import com.example.warehouse.model.WarehouseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseTest {

    private Warehouse wh;
    private Product milk;
    private Product fish;

    private final LocalDate today = LocalDate.of(2025, 1, 15);

    @BeforeEach
    void setUp() {
        wh = new Warehouse();
        wh.addZone(new StorageZone("CH-1", TemperatureMode.CHILLED));
        wh.addZone(new StorageZone("FR-1", TemperatureMode.FROZEN));
        wh.addZone(new StorageZone("AM-1", TemperatureMode.AMBIENT));

        milk = new Product(new Product.ProductId("SKU-M"), "Молоко", Category.MILK);
        fish = new Product(new Product.ProductId("SKU-F"), "Треска", Category.FROZEN_FISH);
        Product bread = new Product(new Product.ProductId("SKU-B"), "Хлеб", Category.CEREALS);
        Product iceCream = new Product(new Product.ProductId("SKU-I"), "Мороженое", Category.ICE_CREAM);

        wh.addProduct(milk);
        wh.addProduct(fish);
        wh.addProduct(bread);
        wh.addProduct(iceCream);

        wh.receiveBatch(milk.id(), new Batch("M-1", today.minusDays(2), today.plusDays(2), 10), "CH-1");
        wh.receiveBatch(milk.id(), new Batch("M-2", today.minusDays(1), today.plusDays(10), 20), "CH-1");
        wh.receiveBatch(milk.id(), new Batch("M-OLD", today.minusDays(10), today.minusDays(1), 5), "CH-1");

        wh.receiveBatch(fish.id(), new Batch("F-1", today.minusDays(30), today.plusDays(5), 15), "FR-1");
        wh.receiveBatch(fish.id(), new Batch("F-2", today.minusDays(10), today.plusDays(30), 25), "FR-1");

        wh.receiveBatch(bread.id(), new Batch("B-1", today.minusDays(1), today.plusDays(3), 8), "AM-1");
        wh.receiveBatch(bread.id(), new Batch("B-2", today.minusDays(1), today.plusDays(7), 12), "AM-1");

        wh.receiveBatch(iceCream.id(), new Batch("I-1", today.minusDays(60), today.plusDays(300), 50), "FR-1");
    }

    @Test
    void receiveBatchAddsBatch() {
        assertEquals(3, wh.batchNumbers(milk.id()).size());
        assertEquals(2, wh.batchNumbers(fish.id()).size());
        assertEquals(2, wh.batchNumbers(new Product.ProductId("SKU-B")).size());
        assertEquals(1, wh.batchNumbers(new Product.ProductId("SKU-I")).size());
    }

    @Test
    void stockOnIgnoresExpired() {
        assertEquals(30, wh.stockOn(milk.id(), today));
        assertEquals(40, wh.stockOn(fish.id(), today));
        assertEquals(20, wh.stockOn(new Product.ProductId("SKU-B"), today));
        assertEquals(50, wh.stockOn(new Product.ProductId("SKU-I"), today));
    }

    @Test
    void expiredListContainsOnlyExpired() {
        List<Batch> expiredMilk = wh.expiredOn(milk.id(), today);
        assertEquals(1, expiredMilk.size());
        assertEquals("M-OLD", expiredMilk.get(0).number());

        assertTrue(wh.expiredOn(fish.id(), today).isEmpty());
        assertTrue(wh.expiredOn(new Product.ProductId("SKU-B"), today).isEmpty());
        assertTrue(wh.expiredOn(new Product.ProductId("SKU-I"), today).isEmpty());
    }

    @Test
    void fefoShipsSoonestFirst() {
        Shipment sh = wh.shipFefo(milk.id(), 15, today);
        assertEquals(2, sh.items().size());
        assertEquals("M-1", sh.items().get(0).batchNumber());
        assertEquals("M-2", sh.items().get(1).batchNumber());

        Shipment shFish = wh.shipFefo(fish.id(), 20, today);
        assertEquals("F-1", shFish.items().get(0).batchNumber());
        assertEquals("F-2", shFish.items().get(1).batchNumber());
    }

    @Test
    void fefoReducesStock() {
        wh.shipFefo(milk.id(), 15, today);
        wh.shipFefo(fish.id(), 10, today);
        wh.shipFefo(new Product.ProductId("SKU-B"), 5, today);

        assertEquals(15, wh.stockOn(milk.id(), today));
        assertEquals(30, wh.stockOn(fish.id(), today));
        assertEquals(15, wh.stockOn(new Product.ProductId("SKU-B"), today));
        assertEquals(50, wh.stockOn(new Product.ProductId("SKU-I"), today));
    }

    @Test
    void overShipThrowsInsufficientStock() {
        WarehouseException exMilk = assertThrows(WarehouseException.class,
                () -> wh.shipFefo(milk.id(), 1000, today));
        assertEquals(WarehouseException.Reason.INSUFFICIENT_STOCK, exMilk.reason());

        WarehouseException exFish = assertThrows(WarehouseException.class,
                () -> wh.shipFefo(fish.id(), 1000, today));
        assertEquals(WarehouseException.Reason.INSUFFICIENT_STOCK, exFish.reason());
    }

    @Test
    void shipExpiredOnlyThrows() {
        wh.shipFefo(milk.id(), 30, today);

        WarehouseException ex = assertThrows(WarehouseException.class,
                () -> wh.ensureNotExpired(milk.id(), 1, today));
        assertEquals(WarehouseException.Reason.EXPIRED_BATCH, ex.reason());

        assertDoesNotThrow(() -> wh.ensureNotExpired(fish.id(), 10, today));
    }

    @Test
    void expiringReportFindsSoonest() {
        var report = wh.expiringReport(today, 15);

        assertTrue(report.keySet().stream().anyMatch(b -> b.number().equals("M-1")));
        assertTrue(report.keySet().stream().anyMatch(b -> b.number().equals("M-2")));
        assertTrue(report.keySet().stream().anyMatch(b -> b.number().equals("B-1")));
        assertTrue(report.keySet().stream().anyMatch(b -> b.number().equals("B-2")));
        assertTrue(report.keySet().stream().anyMatch(b -> b.number().equals("F-1")));

        assertFalse(report.keySet().stream().anyMatch(b -> b.number().equals("I-1")));
        assertFalse(report.keySet().stream().anyMatch(b -> b.number().equals("M-OLD")));
    }

    @Test
    void incompatibleZoneThrows() {
        WarehouseException exMilk = assertThrows(WarehouseException.class,
                () -> wh.receiveBatch(milk.id(),
                        new Batch("B-X", today, today.plusDays(1), 1), "AM-1"));
        assertEquals(WarehouseException.Reason.INCOMPATIBLE_TEMPERATURE, exMilk.reason());

        WarehouseException exFish = assertThrows(WarehouseException.class,
                () -> wh.receiveBatch(fish.id(),
                        new Batch("B-Y", today, today.plusDays(1), 1), "CH-1"));
        assertEquals(WarehouseException.Reason.INCOMPATIBLE_TEMPERATURE, exFish.reason());
    }

    @Test
    void unknownProductThrows() {
        WarehouseException ex = assertThrows(WarehouseException.class,
                () -> wh.shipFefo(new Product.ProductId("NOPE"), 1, today));
        assertEquals(WarehouseException.Reason.UNKNOWN_PRODUCT, ex.reason());
    }

    @Test
    void reservationCancelledByTtl() {
        LocalDate realToday = LocalDate.now();
        wh.receiveBatch(milk.id(), new Batch("M-REAL", realToday.minusDays(1), realToday.plusDays(30), 100), "CH-1");
        wh.receiveBatch(fish.id(), new Batch("F-REAL", realToday.minusDays(1), realToday.plusDays(30), 100), "FR-1");

        Instant now = Instant.now();
        wh.reserve("ORD-M", milk.id(), 10, 1, now);
        wh.reserve("ORD-F", fish.id(), 15, 1, now);
        assertEquals(2, wh.activeReservations().size());

        Instant later = now.plusSeconds(5);
        wh.cancelExpiredReservation("ORD-M", later);
        wh.cancelExpiredReservation("ORD-F", later);
        assertEquals(0, wh.activeReservations().size());
    }

    @Test
    void cancelNotExpiredThrows() {
        LocalDate realToday = LocalDate.now();
        wh.receiveBatch(milk.id(), new Batch("M-REAL2", realToday.minusDays(1), realToday.plusDays(30), 100), "CH-1");

        Instant now = Instant.now();
        wh.reserve("ORD-M2", milk.id(), 5, 3600, now);

        WarehouseException ex = assertThrows(WarehouseException.class,
                () -> wh.cancelExpiredReservation("ORD-M2", now));
        assertEquals(WarehouseException.Reason.RESERVATION_EXPIRED, ex.reason());
    }

    @Test
    void productIdEqualsHashCode() {
        var a = new Product.ProductId("SKU-1");
        var b = new Product.ProductId("SKU-1");
        var c = new Product.ProductId("SKU-2");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "SKU-1");

        assertEquals(milk.id(), new Product.ProductId("SKU-M"));
        assertNotEquals(milk.id(), fish.id());
    }

    @Test
    void batchValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new Batch("", today, today.plusDays(1), 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Batch("B", today.plusDays(5), today, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Batch("B", today, today.plusDays(1), -1));
        assertThrows(IllegalArgumentException.class,
                () -> new Product.ProductId(""));
    }
}