package com.hacom.orders.application.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenTelemetry tracing with Micrometer.
 * Provides a Tracer bean and utility methods for creating spans.
 */
@Configuration
public class TracingConfig {

    private static final Logger log = LoggerFactory.getLogger(TracingConfig.class);

    /**
     * Creates a new span for a given operation, setting the order ID as an attribute.
     */
    public Span createOrderSpan(Tracer tracer, String operationName, String orderId) {
        Span span = tracer.nextSpan().name(operationName);
        span.tag("order.id", orderId);
        span.start();
        log.debug("Started trace span: {} for order: {}", operationName, orderId);
        return span;
    }

    /**
     * Creates a span for the SMS sending operation.
     */
    public Span createSmsSpan(Tracer tracer, String destination) {
        Span span = tracer.nextSpan().name("sendSms");
        span.tag("sms.destination", destination);
        span.start();
        return span;
    }
}
