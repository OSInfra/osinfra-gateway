package com.github.osinfra.gateway.handler;

import org.springframework.cloud.gateway.filter.GatewayFilter;

public interface GatewayFilterProvider {

    GatewayFilter gatewayFilter(String service);

}
