package com.example.warehouse.model;

public class WarehouseException extends RuntimeException {

    public enum Reason {
        UNKNOWN_PRODUCT,
        
        UNKNOWN_ZONE,
        
        INCOMPATIBLE_TEMPERATURE,
       
        INSUFFICIENT_STOCK,
       
        EXPIRED_BATCH,
        
        RESERVATION_NOT_FOUND,
        
        RESERVATION_EXPIRED,
       
        INVALID_ARGUMENT
    }

    private final Reason reason;

    public WarehouseException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}