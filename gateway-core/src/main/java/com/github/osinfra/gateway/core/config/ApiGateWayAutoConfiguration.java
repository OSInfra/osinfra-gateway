package com.github.osinfra.gateway.core.config;

import com.github.osinfra.gateway.core.filter.AuthorizeTokenFilter;
import com.github.osinfra.gateway.core.filter.GatewayFilterProvider;
import com.github.osinfra.gateway.core.handler.ApiRegistry;
import com.github.osinfra.gateway.core.handler.DiscoveryApiWatcher;
import com.github.osinfra.gateway.core.handler.DiscoveryServiceWatcher;
import com.github.osinfra.gateway.core.locator.ApiCacheRouterLocator;
import com.github.osinfra.gateway.core.locator.ApiRouteLocator;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class ApiGateWayAutoConfiguration {

    @Bean
    public ApiRegistry apiRegistry() {
        return new ApiRegistry();
    }

    @Bean
    public ApiCacheRouterLocator apiRouteLocator(ListableBeanFactory beanFactory) {
        ApiRouteLocator apiRouteLocator = new ApiRouteLocator(beanFactory);
        return new ApiCacheRouterLocator(apiRouteLocator);
    }

    @Bean
    public DiscoveryApiWatcher apiReloader(ApiRegistry apiRegistry) {
        return new DiscoveryApiWatcher(apiRegistry);
    }

    @Bean
    public DiscoveryServiceWatcher discoveryServiceWatcher(DiscoveryClient discoveryClient) {
        return new DiscoveryServiceWatcher(discoveryClient);
    }

    @Configuration
    @ConditionalOnProperty(name = "apigateway.authorize.enabled", havingValue = "true")
    public static class TokenConfiguration {

        @Bean
        public AuthorizeTokenFilter defaultAuthorizeFilter() {
            return new AuthorizeTokenFilter() {
                @Override
                public Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain) {
                    return chain.filter(exchange);
                }
            };
        }

        @Bean
        public GatewayFilterProvider authorizeGatewayFilter(AuthorizeTokenFilter authorizeTokenFilter) {
            return provider -> authorizeTokenFilter;
        }
    }

//    public static class A extends AuthorizeTokenFilter {
//
//        @Override
//        public Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain) {
//            System.out.println("into A ");
//            return chain.filter(exchange);
//        }
//    }
//
//    @Bean
//    @Primary
//    public A a() {
//        return new A();
//    }

}
