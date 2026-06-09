package com.hacom.orders;

import com.hacom.orders.domain.model.Order;
import com.hacom.orders.domain.port.OrderRepository;
import com.hacom.orders.infrastructure.persistence.MongoOrderRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@ExtendWith(SpringExtension.class)
@DataMongoTest
@Import(MongoOrderRepository.class)
public class MongoOrderRepositoryTest {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        // Clean the collection before each test
        reactiveMongoTemplate.dropCollection(Order.class).block();
    }

    @Test
    void testSaveAndFindByOrderId() {
        Order order = new Order(
                "ORD-001",
                "CUST-001",
                "+584141234567",
                "PROCESSED",
                List.of("Item1", "Item2"),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        Mono<Order> saveThenFind = orderRepository.save(order)
                .then(orderRepository.findByOrderId("ORD-001"));

        StepVerifier.create(saveThenFind)
                .assertNext(foundOrder -> {
                    assert foundOrder.getOrderId().equals("ORD-001");
                    assert foundOrder.getStatus().equals("PROCESSED");
                    assert foundOrder.getCustomerId().equals("CUST-001");
                    assert foundOrder.getItems().size() == 2;
                    assert foundOrder.getTs() != null;
                })
                .verifyComplete();
    }

    @Test
    void testFindByOrderId_NotFound() {
        StepVerifier.create(orderRepository.findByOrderId("NONEXISTENT"))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testCountByTsBetween() {
        OffsetDateTime baseTime = OffsetDateTime.now(ZoneOffset.UTC);

        // Insert 3 orders at different times
        Order order1 = new Order("ORD-001", "CUST-001", "+584141234567", "PROCESSED",
                List.of("Item1"), baseTime.minusDays(2));
        Order order2 = new Order("ORD-002", "CUST-002", "+584147654321", "PROCESSED",
                List.of("Item2"), baseTime.minusDays(1));
        Order order3 = new Order("ORD-003", "CUST-003", "+584141112233", "PROCESSED",
                List.of("Item3"), baseTime.plusDays(1));

        orderRepository.save(order1).block();
        orderRepository.save(order2).block();
        orderRepository.save(order3).block();

        // Count orders between (baseTime - 3 days) and (baseTime)
        OffsetDateTime from = baseTime.minusDays(3);
        OffsetDateTime to = baseTime;

        StepVerifier.create(orderRepository.countByTsBetween(from, to))
                .expectNext(2L)
                .verifyComplete();
    }
}