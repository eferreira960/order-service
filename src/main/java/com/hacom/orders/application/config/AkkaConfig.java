package com.hacom.orders.application.config;

import akka.actor.ActorSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AkkaConfig {

    private static final Logger log = LoggerFactory.getLogger(AkkaConfig.class);

    @Bean
    public ActorSystem actorSystem() {
        log.info("Creating Akka Actor System");
        return ActorSystem.create("OrderProcessingSystem");
    }
}