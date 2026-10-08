package com.example.warehouse.model;

import java.time.LocalDate;
import java.util.List;

public sealed interface Shipment permits Shipment.Fefo, Shipment.Manual {

    LocalDate date();

    List<ShippedItem> items();

    int totalQuantity();

    record Fefo(LocalDate date, List<ShippedItem> items) implements Shipment {
        public Fefo {
            items = List.copyOf(items);
        }

        @Override
        public int totalQuantity() {
            return items.stream().mapToInt(ShippedItem::quantity).sum();
        }
    }

    record Manual(LocalDate date, List<ShippedItem> items, String reason) implements Shipment {
        public Manual {
            items = List.copyOf(items);
        }

        @Override
        public int totalQuantity() {
            return items.stream().mapToInt(ShippedItem::quantity).sum();
        }
    }

    record ShippedItem(String batchNumber, int quantity) {
        public ShippedItem {
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity > 0");
            }
        }
    }
}