package com.hacom.orders.infrastructure.rest;

import com.hacom.orders.domain.model.Order;
import com.hacom.orders.domain.port.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "API for order management")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/{orderId}/status")
    @Operation(summary = "Get order status by order ID", description = "Returns the current status of an order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public Mono<ResponseEntity<Map<String, String>>> getOrderStatus(
            @Parameter(description = "Order ID to query") @PathVariable String orderId) {
        log.info("REST request received - GET order status for orderId: {}", orderId);

        return orderRepository.findByOrderId(orderId)
                .map(order -> {
                    log.debug("Order found: {} with status: {}", orderId, order.getStatus());
                    return ResponseEntity.ok(Map.of(
                            "orderId", order.getOrderId(),
                            "status", order.getStatus(),
                            "customerId", order.getCustomerId(),
                            "customerPhoneNumber", order.getCustomerPhoneNumber(),
                            "ts", order.getTs().toString()
                    ));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(error -> log.error("Error fetching order status for {}: {}", orderId, error.getMessage()));
    }

    @GetMapping("/count")
    @Operation(summary = "Get total orders by date range", description = "Returns the count of orders between two dates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    })
    public Mono<ResponseEntity<Map<String, Object>>> getOrderCountByDateRange(
            @Parameter(description = "Start date (ISO-8601 offset datetime format)") @RequestParam("from") String from,
            @Parameter(description = "End date (ISO-8601 offset datetime format)") @RequestParam("to") String to) {
        log.info("REST request received - GET order count from: {} to: {}", from, to);

        try {
            OffsetDateTime fromDate = OffsetDateTime.parse(from);
            OffsetDateTime toDate = OffsetDateTime.parse(to);

            return orderRepository.countByTsBetween(fromDate, toDate)
                    .map(count -> {
                        log.debug("Found {} orders between {} and {}", count, from, to);
                        return ResponseEntity.ok(Map.of(
                                "from", from,
                                "to", to,
                                "total", count
                        ));
                    })
                    .doOnError(error -> log.error("Error counting orders: {}", error.getMessage()));
        } catch (Exception e) {
            log.error("Invalid date format in request: from={}, to={}", from, to);
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid date format. Use ISO-8601 offset datetime format (e.g., 2024-01-01T00:00:00Z)"
            )));
        }
    }
}