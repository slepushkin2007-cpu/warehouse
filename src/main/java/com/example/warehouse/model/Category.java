package com.example.warehouse.model;

public enum Category {
    FROZEN_FISH(TemperatureMode.FROZEN, 180),
    ICE_CREAM(TemperatureMode.FROZEN, 365),
    MILK(TemperatureMode.CHILLED, 7),
    YOGURT(TemperatureMode.CHILLED, 14) {
        @Override
        public String describe() {
            return "Кисломолочный продукт: " + super.describe();
        }
    },
    CEREALS(TemperatureMode.AMBIENT, 720),
    CANNED_FOOD(TemperatureMode.AMBIENT, 1095);

    private final TemperatureMode mode;
    private final int defaultShelfLifeDays;

    Category(TemperatureMode mode, int defaultShelfLifeDays) {
        this.mode = mode;
        this.defaultShelfLifeDays = defaultShelfLifeDays;
    }

    public TemperatureMode mode() {
        return mode;
    }

    public int defaultShelfLifeDays() {
        return defaultShelfLifeDays;
    }

    public String describe() {
        return name() + " (" + mode + ", " + defaultShelfLifeDays + " дн.)";
    }
}