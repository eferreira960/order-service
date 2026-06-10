package com.hacom.orders.infrastructure.audit;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;

@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    private String orderId;
    private String action;
    private String actor;
    private String details;
    private String result;
    private String traceId;
    private OffsetDateTime timestamp;

    public AuditLog() {
    }

    public AuditLog(String orderId, String action, String actor, String details,
                    String result, String traceId) {
        this.orderId = orderId;
        this.action = action;
        this.actor = actor;
        this.details = details;
        this.result = result;
        this.traceId = traceId;
        this.timestamp = OffsetDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}