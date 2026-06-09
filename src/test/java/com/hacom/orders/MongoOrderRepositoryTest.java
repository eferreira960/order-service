package com.hacom.orders;

import com.hacom.orders.domain.model.Order;
import com.hacom.orders.domain.model.vo.CustomerId;
import com.hacom.orders.domain.model.vo.OrderId;
import com.hacom.orders.domain.model.vo.PhoneNumber;
import com.hacom.orders.domain.port.OrderRepository;
import com.hacom.orders.infrastructure.persistence.MongoOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Import({MongoOrderRepository.class, TestMongoConfig.class})
public class MongoOrderRepositoryTest {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        reactiveMongoTemplate.dropCollection(Order.class).block();
    }

    @Test
    void testSaveAndFindByOrderId() {
        Order order = Order.create(
                new OrderId("ORD-001"),
                new CustomerId("CUST-001"),
                new PhoneNumber("+584141234567"),
                List.of("Item1", "Item2")
        );

        Mono<Order> saveThenFind = orderRepository.save(order)
                .then(orderRepository.findByOrderId("ORD-001"));

        StepVerifier.create(saveThenFind)
                .assertNext(foundOrder -> {
                    assertEquals("ORD-001", foundOrder.getOrderId());
                    assertEquals("CUST-001", foundOrder.getCustomerId());
                    assertEquals(2, foundOrder.getItems().size());
                    assertNotNull(foundOrder.getTs());
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
        Order order1 = Order.create(
                new OrderId("ORD-001"), new CustomerId("CUST-001"),
                new PhoneNumber("+584141234567"), List.of("Item1"));
        Order order2 = Order.create(
                new OrderId("ORD-002"), new CustomerId("CUST-002"),
                new PhoneNumber("+584147654321"), List.of("Item2"));
        Order order3 = Order.create(
                new OrderId("ORD-003"), new CustomerId("CUST-003"),
                new PhoneNumber("+584141112233"), List.of("Item3"));

        orderRepository.save(order1).block();
        orderRepository.save(order2).block();
        orderRepository.save(order3).block();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(3);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);

        StepVerifier.create(orderRepository.countByTsBetween(from, to))
                .expectNext(3L)
                .verifyComplete();
    }
}