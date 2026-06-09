package com.hacom.orders.application.config;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebFluxConfig.class);

    @Value("${apiPort}")
    private int apiPort;

    @Bean
    public ReactiveWebServerFactory reactiveWebServerFactory() {
        log.info("Configuring WebFlux server port to: {}", apiPort);
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
        factory.setPort(apiPort);
        return factory;
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .compress(true);
    }

    @Bean
    public ReactorClientHttpConnector reactorClientHttpConnector(HttpClient httpClient) {
        return new ReactorClientHttpConnector(httpClient);
    }
}