package com.hacom.orders.infrastructure.audit;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class AuditLogRepository {

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public AuditLogRepository(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    public Mono<AuditLog> save(AuditLog auditLog) {
        return reactiveMongoTemplate.save(auditLog);
    }
}