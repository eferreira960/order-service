package com.hacom.orders.domain.model.vo;

import java.util.Objects;

/**
 * Value Object for Order ID with validation.
 */
public record OrderId(String value) {

    public OrderId {
        Objects.requireNonNull(value, "OrderId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("OrderId must not be blank");
        }
        if (!value.matches("^[A-Za-z0-9_-]{1,50}$")) {
            throw new IllegalArgumentException("OrderId must match pattern: alphanumeric, hyphens, underscores (max 50 chars)");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}