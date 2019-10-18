package com.github.osinfra.gateway.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

@Slf4j
public class ApiRouterLocator implements RouteLocator, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private RouteLocatorBuilder routeLocatorBuilder;



    @Override
    public Flux<Route> getRoutes() {
        return null;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
