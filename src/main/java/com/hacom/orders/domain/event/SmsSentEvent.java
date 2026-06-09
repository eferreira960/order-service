package com.hacom.orders.domain.event;

import com.hacom.orders.domain.model.vo.OrderId;
import com.hacom.orders.domain.model.vo.PhoneNumber;

import java.time.OffsetDateTime;

/**
 * Domain event triggered when an SMS notification has been sent.
 */
public class SmsSentEvent implements DomainEvent {

    private final OrderId orderId;
    private final PhoneNumber destination;
    private final String messageId;
    private final OffsetDateTime recordedAt;

    public SmsSentEvent(OrderId orderId, PhoneNumber destination, String messageId) {
        this.orderId = orderId;
        this.destination = destination;
        this.messageId = messageId;
        this.recordedAt = OffsetDateTime.now();
    }

    @Override
    public String getAggregateId() {
        return orderId.value();
    }

    @Override
    public String getEventType() {
        return "SMS_SENT";
    }

    @Override
    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public PhoneNumber getDestination() {
        return destination;
    }

    public String getMessageId() {
        return messageId;
    }
}