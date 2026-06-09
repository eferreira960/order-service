package com.hacom.orders.application.usecase.impl;

import com.hacom.orders.application.usecase.GetOrderStatusUseCase;
import com.hacom.orders.domain.model.vo.OrderId;
import com.hacom.orders.domain.port.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GetOrderStatusUseCaseImpl implements GetOrderStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetOrderStatusUseCaseImpl.class);

    private final OrderRepository orderRepository;

    public GetOrderStatusUseCaseImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Mono<OrderResult> execute(OrderId orderId) {
        log.debug("Getting status for order: {}", orderId);
        return orderRepository.findByOrderId(orderId.value())
                .map(order -> new OrderResult(
                        order.getOrderId(),
                        order.getStatus(),
                        order.getCustomerId(),
                        order.getCustomerPhoneNumber(),
                        order.getTs() != null ? order.getTs().toString() : null
                ))
                .doOnNext(result -> log.info("Order {} status retrieved: {}", orderId, result.status()))
                .doOnError(error -> log.error("Error retrieving status for order {}: {}", orderId, error.getMessage()));
    }
}