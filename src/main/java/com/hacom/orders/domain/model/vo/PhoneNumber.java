package com.hacom.orders.domain.model.vo;

import java.util.Objects;

public record PhoneNumber(String value) {

    public PhoneNumber {
        Objects.requireNonNull(value, "PhoneNumber must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("PhoneNumber must not be blank");
        }
        // E.164 format validation: + followed by country code and number (7-15 digits)
        if (!value.matches("^\\+[1-9]\\d{6,14}$")) {
            throw new IllegalArgumentException(
                    "PhoneNumber must be in E.164 format (e.g., +584141234567)");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}