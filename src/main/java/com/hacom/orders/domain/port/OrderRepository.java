package com.hacom.orders.domain.port;

import com.hacom.orders.domain.model.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface OrderRepository {

    Mono<Order> save(Order order);

    Mono<Order> findByOrderId(String orderId);

    Mono<Long> countByTsBetween(Instant from, Instant to);
}