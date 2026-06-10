package com.hacom.orders;

import com.hacom.orders.domain.model.Order;
import com.hacom.orders.domain.model.vo.CustomerId;
import com.hacom.orders.domain.model.vo.OrderId;
import com.hacom.orders.domain.model.vo.PhoneNumber;
import com.hacom.orders.infrastructure.persistence.MongoOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MongoOrderRepositoryTest {

    @Mock
    private ReactiveMongoTemplate reactiveMongoTemplate;

    private MongoOrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository = new MongoOrderRepository(reactiveMongoTemplate);
    }

    @Test
    void testSaveAndFindByOrderId() {
        Order order = Order.create(
                new OrderId("ORD-001"),
                new CustomerId("CUST-001"),
                new PhoneNumber("+584141234567"),
                List.of("Item1", "Item2")
        );

        when(reactiveMongoTemplate.save(order)).thenReturn(Mono.just(order));
        when(reactiveMongoTemplate.findOne(any(Query.class), eq(Order.class)))
                .thenReturn(Mono.just(order));

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
        when(reactiveMongoTemplate.findOne(any(Query.class), eq(Order.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(orderRepository.findByOrderId("NONEXISTENT"))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testCountByTsBetween() {
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(3);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);

        when(reactiveMongoTemplate.count(any(Query.class), eq(Order.class)))
                .thenReturn(Mono.just(3L));

        StepVerifier.create(orderRepository.countByTsBetween(from, to))
                .expectNext(3L)
                .verifyComplete();
    }
}