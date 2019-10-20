package com.github.osinfra.gateway.locator;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

public class ApiCacheRouterLocator implements RouteLocator, ApplicationEventPublisherAware {

    private ApplicationEventPublisher applicationEventPublisher;

    private Flux<Route> routes;

    @Override
    public Flux<Route> getRoutes() {
        return this.routes;
    }

    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
