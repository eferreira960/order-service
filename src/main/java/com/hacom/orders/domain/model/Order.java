package com.hacom.orders.domain.model;

import com.hacom.orders.domain.event.DomainEvent;
import com.hacom.orders.domain.event.OrderCreatedEvent;
import com.hacom.orders.domain.event.OrderProcessedEvent;
import com.hacom.orders.domain.event.SmsSentEvent;
import com.hacom.orders.domain.model.vo.CustomerId;
import com.hacom.orders.domain.model.vo.OrderId;
import com.hacom.orders.domain.model.vo.OrderStatus;
import com.hacom.orders.domain.model.vo.PhoneNumber;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for the Order bounded context.
 * Encapsulates all order-related business rules and domain events.
 */
@Document(collection = "orders")
public class Order {

    @Id
    private ObjectId _id;

    private OrderId orderId;
    private CustomerId customerId;
    private PhoneNumber customerPhoneNumber;
    private OrderStatus status;
    private List<String> items;
    private OffsetDateTime ts;

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // Required for Spring Data MongoDB
    protected Order() {
    }

    private Order(OrderId orderId, CustomerId customerId, PhoneNumber customerPhoneNumber,
                  OrderStatus status, List<String> items, OffsetDateTime ts) {
        this._id = new ObjectId();
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerPhoneNumber = customerPhoneNumber;
        this.status = status;
        this.items = Collections.unmodifiableList(items);
        this.ts = ts;
    }

    /**
     * Factory method to create a new Order aggregate.
     * Validates business rules and records OrderCreatedEvent.
     */
    public static Order create(OrderId orderId, CustomerId customerId,
                               PhoneNumber customerPhoneNumber, List<String> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Order order = new Order(
                orderId,
                customerId,
                customerPhoneNumber,
                OrderStatus.PENDING,
                items,
                OffsetDateTime.now()
        );

        order.domainEvents.add(new OrderCreatedEvent(orderId, customerId, customerPhoneNumber, items));
        return order;
    }

    /**
     * Processes the order, transitioning from PENDING to PROCESSED.
     * Records OrderProcessedEvent.
     */
    public OrderProcessedEvent process() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot process order " + orderId + ": current status is " + status);
        }

        if (!this.status.canTransitionTo(OrderStatus.PROCESSED)) {
            throw new IllegalStateException(
                    "Invalid transition from " + this.status + " to " + OrderStatus.PROCESSED);
        }

        this.status = OrderStatus.PROCESSED;
        this.ts = OffsetDateTime.now();

        OrderProcessedEvent event = new OrderProcessedEvent(this.orderId, this.status);
        this.domainEvents.add(event);
        return event;
    }

    /**
     * Marks the order as failed.
     */
    public void fail() {
        if (this.status.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot fail order " + orderId + ": already in terminal state " + status);
        }
        this.status = OrderStatus.FAILED;
        this.ts = OffsetDateTime.now();
    }

    /**
     * Records that an SMS was sent for this order.
     */
    public SmsSentEvent recordSmsSent(PhoneNumber destination, String messageId) {
        SmsSentEvent event = new SmsSentEvent(this.orderId, destination, messageId);
        this.domainEvents.add(event);
        return event;
    }

    /**
     * Clears and returns all recorded domain events.
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }

    // Getters and Setters for MongoDB persistence

    public ObjectId get_id() {
        return _id;
    }

    public void set_id(ObjectId _id) {
        this._id = _id;
    }

    public String getOrderId() {
        return orderId != null ? orderId.value() : null;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId != null ? new OrderId(orderId) : null;
    }

    public String getCustomerId() {
        return customerId != null ? customerId.value() : null;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId != null ? new CustomerId(customerId) : null;
    }

    public String getCustomerPhoneNumber() {
        return customerPhoneNumber != null ? customerPhoneNumber.value() : null;
    }

    public void setCustomerPhoneNumber(String customerPhoneNumber) {
        this.customerPhoneNumber = customerPhoneNumber != null ? new PhoneNumber(customerPhoneNumber) : null;
    }

    public String getStatus() {
        return status != null ? status.getValue() : null;
    }

    public void setStatus(String status) {
        this.status = status != null ? OrderStatus.fromValue(status) : null;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public OffsetDateTime getTs() {
        return ts;
    }

    public void setTs(OffsetDateTime ts) {
        this.ts = ts;
    }

    // Domain getters (return Value Objects)
    public OrderId getOrderIdVO() {
        return orderId;
    }

    public CustomerId getCustomerIdVO() {
        return customerId;
    }

    public PhoneNumber getCustomerPhoneNumberVO() {
        return customerPhoneNumber;
    }

    public OrderStatus getOrderStatus() {
        return status;
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    @Override
    public String toString() {
        return "Order{" +
                "_id=" + _id +
                ", orderId=" + orderId +
                ", customerId=" + customerId +
                ", customerPhoneNumber=" + customerPhoneNumber +
                ", status=" + status +
                ", items=" + items +
                ", ts=" + ts +
                '}';
    }
}