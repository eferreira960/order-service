package com.hacom.orders;

import com.hacom.orders.application.usecase.GetOrderStatusUseCase;
import com.hacom.orders.domain.model.vo.OrderId;
import com.hacom.orders.domain.port.OrderRepository;
import com.hacom.orders.infrastructure.rest.OrderController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = OrderController.class)
@DisplayName("REST Controller Tests (Unitarios con Mocks)")
public class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GetOrderStatusUseCase getOrderStatusUseCase;

    @MockBean
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("GET /api/orders/{orderId}/status - Order exists returns 200")
    void testGetOrderStatus_Found() {
        when(getOrderStatusUseCase.execute(new OrderId("ORD-001")))
                .thenReturn(Mono.just(new GetOrderStatusUseCase.OrderResult(
                        "ORD-001", "PROCESSED", "CUST-001",
                        "+584141234567", OffsetDateTime.now().toString()
                )));

        webTestClient.get()
                .uri("/api/orders/ORD-001/status")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderId").isEqualTo("ORD-001")
                .jsonPath("$.status").isEqualTo("PROCESSED")
                .jsonPath("$.customerId").isEqualTo("CUST-001")
                .jsonPath("$.customerPhoneNumber").isEqualTo("+584141234567")
                .jsonPath("$.ts").isNotEmpty();
    }

    @Test
    @DisplayName("GET /api/orders/{orderId}/status - Order not found returns 404")
    void testGetOrderStatus_NotFound() {
        when(getOrderStatusUseCase.execute(new OrderId("ORD-999")))
                .thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/orders/ORD-999/status")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /api/orders/{orderId}/status - Empty orderId returns 404")
    void testGetOrderStatus_EmptyOrderId() {
        webTestClient.get()
                .uri("/api/orders//status")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /api/orders/{orderId}/status - Repository error returns 404")
    void testGetOrderStatus_RepositoryError() {
        when(getOrderStatusUseCase.execute(new OrderId("ORD-ERR")))
                .thenReturn(Mono.error(new RuntimeException("DB connection failed")));

        webTestClient.get()
                .uri("/api/orders/ORD-ERR/status")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /api/orders/{orderId}/status - Status returns PROCESSED correctly")
    void testGetOrderStatus_StatusField() {
        when(getOrderStatusUseCase.execute(new OrderId("ORD-002")))
                .thenReturn(Mono.just(new GetOrderStatusUseCase.OrderResult(
                        "ORD-002", "PROCESSED", "CUST-002",
                        "+584147654321", OffsetDateTime.now().toString()
                )));

        webTestClient.get()
                .uri("/api/orders/ORD-002/status")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("PROCESSED");
    }

    @Test
    @DisplayName("GET /api/orders/{orderId}/status - Status returns FAILED correctly")
    void testGetOrderStatus_StatusFailed() {
        when(getOrderStatusUseCase.execute(new OrderId("ORD-003")))
                .thenReturn(Mono.just(new GetOrderStatusUseCase.OrderResult(
                        "ORD-003", "FAILED", "CUST-003",
                        "+584141112233", OffsetDateTime.now().toString()
                )));

        webTestClient.get()
                .uri("/api/orders/ORD-003/status")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("FAILED")
                .jsonPath("$.orderId").isEqualTo("ORD-003");
    }

    @Test
    @DisplayName("GET /api/orders/count - Valid date range returns count")
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
                .jsonPath("$.total").isEqualTo(5)
                .jsonPath("$.from").isEqualTo(from)
                .jsonPath("$.to").isEqualTo(to);
    }

    @Test
    @DisplayName("GET /api/orders/count - Zero count returns 0")
    void testGetOrderCountByDateRange_ZeroCount() {
        String from = "2023-01-01T00:00:00Z";
        String to = "2023-01-02T00:00:00Z";
        OffsetDateTime fromDate = OffsetDateTime.parse(from);
        OffsetDateTime toDate = OffsetDateTime.parse(to);

        when(orderRepository.countByTsBetween(fromDate, toDate)).thenReturn(Mono.just(0L));

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
                .jsonPath("$.total").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/orders/count - Invalid 'from' date returns 400")
    void testGetOrderCountByDateRange_InvalidFromDate() {
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

    @Test
    @DisplayName("GET /api/orders/count - Invalid 'to' date returns 400")
    void testGetOrderCountByDateRange_InvalidToDate() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders/count")
                        .queryParam("from", "2024-01-01T00:00:00Z")
                        .queryParam("to", "bad-date")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isNotEmpty();
    }

    @Test
    @DisplayName("GET /api/orders/count - Missing 'from' param returns 400")
    void testGetOrderCountByDateRange_MissingFrom() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders/count")
                        .queryParam("to", "2024-12-31T23:59:59Z")
                        .build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /api/orders/count - Missing 'to' param returns 400")
    void testGetOrderCountByDateRange_MissingTo() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders/count")
                        .queryParam("from", "2024-01-01T00:00:00Z")
                        .build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /api/orders/count - Both params missing returns 400")
    void testGetOrderCountByDateRange_MissingBoth() {
        webTestClient.get()
                .uri("/api/orders/count")
                .exchange()
                .expectStatus().isBadRequest();
    }
}