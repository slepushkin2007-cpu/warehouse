package com.example.warehouse.model;

import java.time.LocalDate;
import java.util.Objects;

public record Batch(String number,
                    LocalDate productionDate,
                    LocalDate expiryDate,
                    int quantity) {

    public Batch {
        Objects.requireNonNull(number, "number");
        Objects.requireNonNull(productionDate, "productionDate");
        Objects.requireNonNull(expiryDate, "expiryDate");
        if (number.isBlank()) {
            throw new IllegalArgumentException("batch number must not be blank");
        }
        if (expiryDate.isBefore(productionDate)) {
            throw new IllegalArgumentException("expiryDate cannot be before productionDate");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
    }

    public boolean isExpiredOn(LocalDate date) {
        return expiryDate.isBefore(date);
    }

    public boolean expiresWithin(LocalDate from, int days) {
        LocalDate limit = from.plusDays(days);
        return !expiryDate.isBefore(from) && !expiryDate.isAfter(limit);
    }

    public Batch withQuantity(int newQuantity) {
        return new Batch(number, productionDate, expiryDate, newQuantity);
    }
}