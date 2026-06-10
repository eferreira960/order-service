package com.hacom.orders.domain.model.vo;

public enum OrderStatus {
    PENDING("PENDING"),
    PROCESSED("PROCESSED"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isTerminal() {
        return this == PROCESSED || this == FAILED || this == CANCELLED;
    }

    public boolean canTransitionTo(OrderStatus target) {
        if (this == target) return true;
        if (this == PENDING && target == PROCESSED) return true;
        if (this == PENDING && target == FAILED) return true;
        if (this == PENDING && target == CANCELLED) return true;
        return false;
    }

    public static OrderStatus fromValue(String value) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown OrderStatus: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}