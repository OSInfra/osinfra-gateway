package com.github.osinfra.gateway.core.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class AuthorizeTokenFilter implements GatewayFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return doFilter(exchange, chain);
    }

    public abstract Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain);
}
