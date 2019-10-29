package com.github.osinfra.gateway.core.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;

public interface GatewayFilterProvider {

    GatewayFilter gatewayFilter(String service);

}
