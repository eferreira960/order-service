package com.hacom.orders.domain.model.vo;

import java.util.Objects;

/**
 * Value Object for Customer ID with validation.
 */
public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "CustomerId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CustomerId must not be blank");
        }
        if (!value.matches("^[A-Za-z0-9_-]{1,50}$")) {
            throw new IllegalArgumentException("CustomerId must match pattern: alphanumeric, hyphens, underscores (max 50 chars)");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}