package com.hacom.orders.domain.event;

import com.hacom.orders.domain.model.vo.OrderId;
import com.hacom.orders.domain.model.vo.OrderStatus;

import java.time.OffsetDateTime;

/**
 * Domain event triggered when an order has been processed successfully.
 */
public class OrderProcessedEvent implements DomainEvent {

    private final OrderId orderId;
    private final OrderStatus status;
    private final OffsetDateTime recordedAt;

    public OrderProcessedEvent(OrderId orderId, OrderStatus status) {
        this.orderId = orderId;
        this.status = status;
        this.recordedAt = OffsetDateTime.now();
    }

    @Override
    public String getAggregateId() {
        return orderId.value();
    }

    @Override
    public String getEventType() {
        return "ORDER_PROCESSED";
    }

    @Override
    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }
}