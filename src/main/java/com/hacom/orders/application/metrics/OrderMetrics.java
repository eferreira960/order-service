package com.hacom.orders.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {

    private static final Logger log = LoggerFactory.getLogger(OrderMetrics.class);

    private final Counter ordersProcessedCounter;
    private final Counter ordersFailedCounter;

    public OrderMetrics(MeterRegistry meterRegistry) {
        this.ordersProcessedCounter = Counter.builder("hacom.orders.processed.total")
                .description("Total number of orders processed")
                .tag("status", "success")
                .register(meterRegistry);

        this.ordersFailedCounter = Counter.builder("hacom.orders.processed.total")
                .description("Total number of failed orders")
                .tag("status", "failed")
                .register(meterRegistry);

        log.info("Prometheus metrics initialized: hacom.orders.processed.total");
    }

    public void incrementProcessedOrders() {
        ordersProcessedCounter.increment();
        log.debug("Orders processed counter incremented, current count: {:.0f}", ordersProcessedCounter.count());
    }

    public void incrementFailedOrders() {
        ordersFailedCounter.increment();
        log.debug("Orders failed counter incremented, current count: {:.0f}", ordersFailedCounter.count());
    }
}