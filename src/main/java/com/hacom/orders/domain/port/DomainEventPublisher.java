package com.hacom.orders.domain.port;

import com.hacom.orders.domain.event.DomainEvent;
import reactor.core.publisher.Mono;

/**
 * Port for publishing domain events to external handlers.
 */
public interface DomainEventPublisher {

    Mono<Void> publish(DomainEvent event);
}