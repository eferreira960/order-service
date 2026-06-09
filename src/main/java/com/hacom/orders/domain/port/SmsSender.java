package com.hacom.orders.domain.port;

public interface SmsSender {

    void sendSms(String destinationNumber, String message);
}