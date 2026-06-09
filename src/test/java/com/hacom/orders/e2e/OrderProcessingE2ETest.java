package com.hacom.orders.e2e;

import com.hacom.orders.domain.model.Order;
import com.hacom.orders.domain.port.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E test that validates the complete flow using Testcontainers with a real MongoDB.
 * Tests: Order creation, persistence, REST API querying.
 * Note: gRPC requires a separate gRPC client — this test focuses on the REST + persistence layer.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderProcessingE2ETest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("mongodbUri", mongoDBContainer::getReplicaSetUrl);
        registry.add("mongodbDatabase", () -> "testdb");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Test
    void testE2EOrderFlow() {
        // 1. Setup: clean database
        reactiveMongoTemplate.dropCollection("orders").block();

        // 2. Create test data directly via repository (simulating gRPC + actor flow)
        Order testOrder = new Order();
        testOrder.setOrderId("E2E-ORD-001");
        testOrder.setCustomerId("E2E-CUST-001");
        testOrder.setCustomerPhoneNumber("+584141234567");
        testOrder.setStatus("PROCESSED");
        testOrder.setItems(java.util.List.of("Item1", "Item2"));
        testOrder.setTs(java.time.OffsetDateTime.now());

        // 3. Save order (simulating what the actor does)
        Order savedOrder = orderRepository.save(testOrder).block(Duration.ofSeconds(5));
        assertNotNull(savedOrder);
        assertEquals("E2E-ORD-001", savedOrder.getOrderId());

        // 4. Query via REST API - GET order status
        webTestClient.get()
                .uri("/api/orders/E2E-ORD-001/status")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderId").isEqualTo("E2E-ORD-001")
                .jsonPath("$.status").isEqualTo("PROCESSED")
                .jsonPath("$.customerId").isEqualTo("E2E-CUST-001")
                .jsonPath("$.customerPhoneNumber").isEqualTo("+584141234567");

        // 5. Query via REST API - GET order count by date range
        String from = java.time.OffsetDateTime.now().minusDays(1).toString();
        String to = java.time.OffsetDateTime.now().plusDays(1).toString();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders/count")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(1);

        // 6. Query non-existent order
        webTestClient.get()
                .uri("/api/orders/NONEXISTENT/status")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        // 7. Verify via repository
        Mono<Order> foundOrder = orderRepository.findByOrderId("E2E-ORD-001");
        StepVerifier.create(foundOrder)
                .assertNext(order -> {
                    assertEquals("E2E-ORD-001", order.getOrderId());
                    assertEquals("PROCESSED", order.getStatus());
                })
                .verifyComplete();

        // 8. Clean up
        reactiveMongoTemplate.dropCollection("orders").block();
    }
}