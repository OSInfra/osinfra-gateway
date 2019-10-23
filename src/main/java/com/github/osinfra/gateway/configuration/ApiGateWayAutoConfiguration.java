package com.github.osinfra.gateway.configuration;

import com.github.osinfra.gateway.handler.ApiRegistry;
import com.github.osinfra.gateway.handler.DiscoveryApiWatcher;
import com.github.osinfra.gateway.handler.DiscoveryServiceWatcher;
import com.github.osinfra.gateway.locator.ApiCacheRouterLocator;
import com.github.osinfra.gateway.locator.ApiRouteLocator;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.cloud.alibaba.nacos.NacosDiscoveryClient;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
