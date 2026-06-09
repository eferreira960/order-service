package com.hacom.orders;

import com.hacom.orders.domain.model.Order;
import com.hacom.orders.domain.port.OrderRepository;
import com.hacom.orders.infrastructure.rest.OrderController;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@WebFluxTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderRepository orderRepository;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new Order();
        sampleOrder.set_id(new ObjectId());
        sampleOrder.setOrderId("ORD-001");
        sampleOrder.setCustomerId("CUST-001");
        sampleOrder.setCustomerPhoneNumber("+584141234567");
        sampleOrder.setStatus("PROCESSED");
        sampleOrder.setItems(List.of("Item1", "Item2"));
        sampleOrder.setTs(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void testGetOrderStatus_Found() {
        when(orderRepository.findByOrderId("ORD-001")).thenReturn(Mono.just(sampleOrder));

        webTestClient.get()
                .uri("/api/orders/ORD-001/status")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderId").isEqualTo("ORD-001")
                .jsonPath("$.status").isEqualTo("PROCESSED")
                .jsonPath("$.customerId").isEqualTo("CUST-001");
    }

    @Test
    void testGetOrderStatus_NotFound() {
        when(orderRepository.findByOrderId("ORD-999")).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/orders/ORD-999/status")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testGetOrderCountByDateRange() {
        String from = "2024-01-01T00:00:00Z";
        String to = "2024-12-31T23:59:59Z";
        OffsetDateTime fromDate = OffsetDateTime.parse(from);
        OffsetDateTime toDate = OffsetDateTime.parse(to);

        when(orderRepository.countByTsBetween(fromDate, toDate)).thenReturn(Mono.just(5L));

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
                .jsonPath("$.total").isEqualTo(5);
    }

    @Test
    void testGetOrderCountByDateRange_InvalidDate() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders/count")
                        .queryParam("from", "invalid-date")
                        .queryParam("to", "2024-12-31T23:59:59Z")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isNotEmpty();
    }
}