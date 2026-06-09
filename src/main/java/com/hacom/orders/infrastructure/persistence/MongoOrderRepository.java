package com.hacom.orders.infrastructure.persistence;

import com.hacom.orders.domain.model.Order;
import com.hacom.orders.domain.port.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Repository
public class MongoOrderRepository implements OrderRepository {

    private static final Logger log = LoggerFactory.getLogger(MongoOrderRepository.class);

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public MongoOrderRepository(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    @Override
    public Mono<Order> save(Order order) {
        log.debug("Saving order with ID: {}", order.getOrderId());
        return reactiveMongoTemplate.save(order)
                .doOnSuccess(savedOrder -> log.info("Order saved successfully with ID: {}", savedOrder.getOrderId()))
                .doOnError(error -> log.error("Error saving order with ID: {}: {}", order.getOrderId(), error.getMessage()));
    }

    @Override
    public Mono<Order> findByOrderId(String orderId) {
        log.debug("Finding order by orderId: {}", orderId);
        Query query = new Query(Criteria.where("orderId").is(orderId));
        return reactiveMongoTemplate.findOne(query, Order.class)
                .doOnNext(order -> log.debug("Found order: {}", order.getOrderId()))
                .doFinally(signalType -> {
                    if (signalType == reactor.core.publisher.SignalType.ON_COMPLETE) {
                        log.debug("No order found with orderId: {}", orderId);
                    }
                });
    }

    @Override
    public Mono<Long> countByTsBetween(OffsetDateTime from, OffsetDateTime to) {
        log.debug("Counting orders between {} and {}", from, to);
        Query query = new Query(Criteria.where("ts").gte(from).lte(to));
        return reactiveMongoTemplate.count(query, Order.class)
                .doOnNext(count -> log.debug("Found {} orders in date range", count));
    }
}