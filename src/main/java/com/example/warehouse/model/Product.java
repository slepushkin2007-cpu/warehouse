package com.example.warehouse.model;

import java.util.Objects;


public record Product(ProductId id, String name, Category category) {
    public record ProductId(String sku) {
        public ProductId {
            Objects.requireNonNull(sku, "sku");
            if (sku.isBlank()) {
                throw new IllegalArgumentException("sku must not be blank");
            }
        }
    }

    public Product {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(category, "category");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}