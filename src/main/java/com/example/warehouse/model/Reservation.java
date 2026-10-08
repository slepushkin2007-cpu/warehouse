package com.example.warehouse.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

public final class Reservation {

    private final String orderId;
    private final Product.ProductId productId;
    private final String batchNumber;
    private final int quantity;
    private final Instant createdAt;
    private final Instant expiresAt;
    private boolean cancelled;

    public Reservation(String orderId, Product.ProductId productId,
                       String batchNumber, int quantity, long ttlSeconds) {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(batchNumber, "batchNumber");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity > 0");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("ttlSeconds > 0");
        }
        this.orderId = orderId;
        this.productId = productId;
        this.batchNumber = batchNumber;
        this.quantity = quantity;
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plusSeconds(ttlSeconds);
        this.cancelled = false;
    }

    public String orderId() { return orderId; }

    public Product.ProductId productId() { return productId; }

    public String batchNumber() { return batchNumber; }

    public int quantity() { return quantity; }

    public Instant createdAt() { return createdAt; }

    public Instant expiresAt() { return expiresAt; }

    public boolean isCancelled() { return cancelled; }

    public boolean isExpiredAt(Instant now) {
        return now.isAfter(expiresAt);
    }

    public void cancel() {
        this.cancelled = true;
    }

    public LocalDate createdDate() {
        return LocalDate.ofInstant(createdAt, ZoneId.systemDefault());
    }
}