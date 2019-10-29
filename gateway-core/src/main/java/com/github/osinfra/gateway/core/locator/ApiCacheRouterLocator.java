package com.github.osinfra.gateway.core.locator;

import com.github.osinfra.gateway.core.event.RefreshApiCacheEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.NonNull;
import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ApiCacheRouterLocator implements RouteLocator, ApplicationEventPublisherAware {

    private ApplicationEventPublisher applicationEventPublisher;

    private final Flux<Route> routes;

    private final RouteLocator delegate;

    private final Map<String, List> cache = new HashMap<>();

    public ApiCacheRouterLocator(RouteLocator delegate) {

        this.delegate = delegate;

        routes = CacheFlux.lookup(cache, "routes", Route.class)
                .onCacheMissResume(() -> this.delegate.getRoutes().sort(AnnotationAwareOrderComparator.INSTANCE));

    }

    @Override
    public Flux<Route> getRoutes() {
        return this.routes;
    }

    @EventListener(RefreshApiCacheEvent.class)
    public void handleRefresh(RefreshApiCacheEvent event) {

        this.cache.clear();

        log.info("refresh routes...");

        applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));

    }

    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
