package com.github.osinfra.gateway.locator;

import com.github.osinfra.gateway.factory.PathsPredicateFactory;
import com.github.osinfra.gateway.filter.GatewayFilterProvider;
import com.github.osinfra.gateway.handler.ApiRegistry;
import com.github.osinfra.gateway.model.Api;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.BooleanSpec;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Function;

@Slf4j
public class ApiRouteLocator implements RouteLocator {

    private final ListableBeanFactory beanFactory;

    private RouteLocatorBuilder routeLocatorBuilder;

    private List<GatewayFilterProvider> gatewayFilterProviders;

    private ApiRegistry apiRegistry;

    public ApiRouteLocator(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Flux<Route> getRoutes() {
        RouteLocatorBuilder.Builder builder = getRouteLocatorBuilder().routes();
        ImmutableListMultimap<String, Api> multimap = getApiRegistry().groupByServiceId();
        for (String s : multimap.keySet()) {
            List<Api> apiList = multimap.get(s);
            builder = builder.route(s,
                    predicateSpec -> {
                        BooleanSpec spec = predicateSpec
                                .predicate(new PathsPredicateFactory().apply(config -> config.setApis(apiList)));
                        return spec.filters(getGatewayFilters(s))
                                .uri("lb://" + s)
                                .order(Ordered.LOWEST_PRECEDENCE);
                    });
        }
        return builder.build().getRoutes();
    }

    private List<GatewayFilterProvider> gatewayFilterProviders() {
        if (gatewayFilterProviders == null) {
            gatewayFilterProviders = Lists.newArrayList(beanFactory.getBeansOfType(GatewayFilterProvider.class).values());
        }
        return gatewayFilterProviders;
    }

    private ApiRegistry getApiRegistry() {
        if (apiRegistry == null) {
            apiRegistry = beanFactory.getBean(ApiRegistry.class);
        }
        return apiRegistry;
    }

    private RouteLocatorBuilder getRouteLocatorBuilder() {
        if (routeLocatorBuilder == null) {
            routeLocatorBuilder = beanFactory.getBean(RouteLocatorBuilder.class);
        }
        return routeLocatorBuilder;
    }

    private Function<GatewayFilterSpec, UriSpec> getGatewayFilters(String s) {
        return f -> {
            for (GatewayFilterProvider gatewayFilterProvider : gatewayFilterProviders()) {
                f = f.filter(gatewayFilterProvider.gatewayFilter(s));
            }
            return f;
        };
    }
}
