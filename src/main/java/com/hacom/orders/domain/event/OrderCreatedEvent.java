package com.hacom.orders.domain.event;

import com.hacom.orders.domain.model.vo.CustomerId;
import com.hacom.orders.domain.model.vo.OrderId;
import com.hacom.orders.domain.model.vo.PhoneNumber;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Domain event triggered when an order is created.
 */
public class OrderCreatedEvent implements DomainEvent {

    private final OrderId orderId;
    private final CustomerId customerId;
    private final PhoneNumber customerPhoneNumber;
    private final List<String> items;
    private final OffsetDateTime recordedAt;

    public OrderCreatedEvent(OrderId orderId, CustomerId customerId,
                             PhoneNumber customerPhoneNumber, List<String> items) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerPhoneNumber = customerPhoneNumber;
        this.items = items;
        this.recordedAt = OffsetDateTime.now();
    }

    @Override
    public String getAggregateId() {
        return orderId.value();
    }

    @Override
    public String getEventType() {
        return "ORDER_CREATED";
    }

    @Override
    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public PhoneNumber getCustomerPhoneNumber() {
        return customerPhoneNumber;
    }

    public List<String> getItems() {
        return items;
    }
}