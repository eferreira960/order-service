package com.hacom.orders.infrastructure.actor;

import com.hacom.orders.domain.model.Order;
import com.hacom.orders.grpc.OrderRequest;
import com.hacom.orders.grpc.OrderResponse;
import io.grpc.stub.StreamObserver;

/**
 * Message sent when the order has been successfully saved to MongoDB.
 */
public class OrderSavedMessage {
    private final Order order;
    private final OrderRequest request;
    private final StreamObserver<OrderResponse> responseObserver;

    public OrderSavedMessage(Order order, OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        this.order = order;
        this.request = request;
        this.responseObserver = responseObserver;
    }

    public Order getOrder() { return order; }
    public OrderRequest getRequest() { return request; }
    public StreamObserver<OrderResponse> getResponseObserver() { return responseObserver; }
}