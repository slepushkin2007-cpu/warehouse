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
        wh.addProduct(milk);
        wh.addProduct(fish);

        wh.receiveBatch(milk.id(),
                new Batch("B-1", today.minusDays(2), today.plusDays(2), 10), "CH-1");
        wh.receiveBatch(milk.id(),
                new Batch("B-2", today.minusDays(1), today.plusDays(10), 20), "CH-1");
        wh.receiveBatch(fish.id(),
                new Batch("B-3", today.minusDays(30), today.minusDays(1), 5), "FR-1");
    }

    @Test
    void receiveBatchAddsBatch() {
        assertEquals(2, wh.batchNumbers(milk.id()).size());
    }

    @Test
    void stockOnIgnoresExpired() {
        assertEquals(30, wh.stockOn(milk.id(), today));
        assertEquals(0, wh.stockOn(fish.id(), today));
    }

    @Test
    void expiredListContainsOnlyExpired() {
        List<Batch> expired = wh.expiredOn(fish.id(), today);
        assertEquals(1, expired.size());
        assertEquals("B-3", expired.get(0).number());
    }

    @Test
    void fefoShipsSoonestFirst() {
        Shipment sh = wh.shipFefo(milk.id(), 15, today);
        assertEquals(2, sh.items().size());
        assertEquals("B-1", sh.items().get(0).batchNumber());
        assertEquals(10, sh.items().get(0).quantity());
        assertEquals("B-2", sh.items().get(1).batchNumber());
        assertEquals(5, sh.items().get(1).quantity());
    }

    @Test
    void fefoReducesStock() {
        wh.shipFefo(milk.id(), 15, today);
        assertEquals(15, wh.stockOn(milk.id(), today));
    }

    @Test
    void overShipThrowsInsufficientStock() {
        WarehouseException ex = assertThrows(WarehouseException.class,
                () -> wh.shipFefo(milk.id(), 1000, today));
        assertEquals(WarehouseException.Reason.INSUFFICIENT_STOCK, ex.reason());
    }

    @Test
    void shipExpiredOnlyThrows() {
        WarehouseException ex = assertThrows(WarehouseException.class,
                () -> wh.ensureNotExpired(fish.id(), 1, today));
        assertEquals(WarehouseException.Reason.EXPIRED_BATCH, ex.reason());
    }

    @Test
    void expiringReportFindsSoonest() {
        var report = wh.expiringReport(today, 15);
        assertTrue(report.keySet().stream().anyMatch(b -> b.number().equals("B-1")));
        assertTrue(report.keySet().stream().anyMatch(b -> b.number().equals("B-2")));
        assertFalse(report.keySet().stream().anyMatch(b -> b.number().equals("B-3")));
    }

    @Test
    void incompatibleZoneThrows() {
        WarehouseException ex = assertThrows(WarehouseException.class,
                () -> wh.receiveBatch(milk.id(),
                        new Batch("B-X", today, today.plusDays(1), 1), "AM-1"));
        assertEquals(WarehouseException.Reason.INCOMPATIBLE_TEMPERATURE, ex.reason());
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
        wh.receiveBatch(milk.id(),
                new Batch("B-REAL", realToday.minusDays(1), realToday.plusDays(30), 100), "CH-1");

        Instant now = Instant.now();
        Reservation r = wh.reserve("ORD-X", milk.id(), 5, 1, now);
        assertEquals(1, wh.activeReservations().size());
        assertEquals("ORD-X", r.orderId());

        Instant later = now.plusSeconds(5);
        wh.cancelExpiredReservation("ORD-X", later);
        assertEquals(0, wh.activeReservations().size());
    }

        @Test
    void cancelNotExpiredThrows() {
        LocalDate realToday = LocalDate.now();
        wh.receiveBatch(milk.id(),
                new Batch("B-REAL2", realToday.minusDays(1), realToday.plusDays(30), 100), "CH-1");

        Instant now = Instant.now();
        wh.reserve("ORD-Y", milk.id(), 5, 3600, now);
        WarehouseException ex = assertThrows(WarehouseException.class,
                () -> wh.cancelExpiredReservation("ORD-Y", now));
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