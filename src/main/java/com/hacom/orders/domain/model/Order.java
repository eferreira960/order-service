package com.hacom.orders.domain.model;

import com.hacom.orders.domain.model.vo.CustomerId;
import com.hacom.orders.domain.model.vo.OrderId;
import com.hacom.orders.domain.model.vo.OrderStatus;
import com.hacom.orders.domain.model.vo.PhoneNumber;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Document(collection = "orders")
public class Order {

    @Id
    private ObjectId _id;

    private String orderId;
    private String customerId;
    private String customerPhoneNumber;
    private String status;
    private List<String> items;
    private Instant ts;

    protected Order() {
    }

    private Order(OrderId orderId, CustomerId customerId, PhoneNumber customerPhoneNumber,
                  OrderStatus status, List<String> items, Instant ts) {
        this._id = new ObjectId();
        this.orderId = orderId != null ? orderId.value() : null;
        this.customerId = customerId != null ? customerId.value() : null;
        this.customerPhoneNumber = customerPhoneNumber != null ? customerPhoneNumber.value() : null;
        this.status = status != null ? status.getValue() : null;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.ts = ts;
    }

    public static Order create(OrderId orderId, CustomerId customerId,
                               PhoneNumber customerPhoneNumber, List<String> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        return new Order(
                orderId,
                customerId,
                customerPhoneNumber,
                OrderStatus.PENDING,
                items,
                Instant.now()
        );
    }

    public void process() {
        OrderStatus currentStatus = OrderStatus.fromValue(this.status);
        if (currentStatus != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot process order " + orderId + ": current status is " + status);
        }
        this.status = OrderStatus.PROCESSED.getValue();
        this.ts = Instant.now();
    }

    public void fail() {
        OrderStatus currentStatus = OrderStatus.fromValue(this.status);
        if (currentStatus.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot fail order " + orderId + ": already in terminal state " + status);
        }
        this.status = OrderStatus.FAILED.getValue();
        this.ts = Instant.now();
    }

    public ObjectId get_id() {
        return _id;
    }

    public void set_id(ObjectId _id) {
        this._id = _id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerPhoneNumber() {
        return customerPhoneNumber;
    }

    public void setCustomerPhoneNumber(String customerPhoneNumber) {
        this.customerPhoneNumber = customerPhoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public Instant getTs() {
        return ts;
    }

    public void setTs(Instant ts) {
        this.ts = ts;
    }

    // Domain getters (return Value Objects)

    public OrderId getOrderIdVO() {
        return orderId != null ? new OrderId(orderId) : null;
    }

    public CustomerId getCustomerIdVO() {
        return customerId != null ? new CustomerId(customerId) : null;
    }

    public PhoneNumber getCustomerPhoneNumberVO() {
        return customerPhoneNumber != null ? new PhoneNumber(customerPhoneNumber) : null;
    }

    public OrderStatus getOrderStatus() {
        return status != null ? OrderStatus.fromValue(status) : null;
    }

    @Override
    public String toString() {
        return "Order{" +
                "_id=" + _id +
                ", orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", customerPhoneNumber='" + customerPhoneNumber + '\'' +
                ", status='" + status + '\'' +
                ", items=" + items +
                ", ts=" + ts +
                '}';
    }
}