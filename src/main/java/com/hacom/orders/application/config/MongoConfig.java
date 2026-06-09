package com.hacom.orders.application.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.hacom.orders.infrastructure.persistence")
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${mongodbUri}")
    private String mongodbUri;

    @Value("${mongodbDatabase}")
    private String mongodbDatabase;

    @Override
    protected String getDatabaseName() {
        return mongodbDatabase;
    }

    @Override
    public MongoClient reactiveMongoClient() {
        log.info("Configuring MongoDB client with URI: {} and database: {}", mongodbUri, mongodbDatabase);
        return MongoClients.create(mongodbUri);
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        log.debug("Creating ReactiveMongoTemplate for database: {}", mongodbDatabase);
        return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
    }
}