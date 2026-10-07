package com.example.warehouse.model;

import java.util.Objects;

public record StorageZone(String name, TemperatureMode mode) {
    public StorageZone {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(mode, "mode");
        if (name.isBlank()) {
            throw new IllegalArgumentException("zone name must not be blank");
        }
    }
}