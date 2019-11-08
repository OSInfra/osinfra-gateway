package com.github.osinfra.gateway.client.configuration;

import com.github.osinfra.gateway.client.endpoint.ApiDiscoveryEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GatewayClientProperty.class)
public class GatewayClientAutoConfiguration {

    @Bean
    public ApiDiscoveryEndpoint apiDiscoveryEndpoint(GatewayClientProperty gatewayClientProperty) {
        return new ApiDiscoveryEndpoint(gatewayClientProperty);
    }
}
