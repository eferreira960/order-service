package com.hacom.orders.application.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${apiPort}")
    private int apiPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HACOM Orders Processing API")
                        .version("1.0.0")
                        .description("REST API for order processing system with MongoDB, gRPC, Akka Actors, and SMPP integration.")
                        .contact(new Contact()
                                .name("HACOM Development Team")
                                .email("freddy.mendoza@hacom-tech.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://hacom-tech.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + apiPort)
                                .description("Local development server")
                ));
    }
}