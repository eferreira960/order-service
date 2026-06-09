package com.hacom.orders.domain.event;

import java.time.OffsetDateTime;

/**
 * Base interface for all domain events.
 */
public interface DomainEvent {

    String getAggregateId();

    String getEventType();

    OffsetDateTime getRecordedAt();
}