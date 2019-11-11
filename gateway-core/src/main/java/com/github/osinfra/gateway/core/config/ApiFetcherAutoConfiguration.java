package com.github.osinfra.gateway.core.config;

import com.github.osinfra.gateway.core.fetcher.EndpointApiFetcher;
import com.github.osinfra.gateway.core.fetcher.SwaggerApiFetcher;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(ApiGatewayProperties.class)
public class ApiFetcherAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WebClient webClient(ApiGatewayProperties apiGatewayProperties) {

        TcpClient tcpClient = TcpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, apiGatewayProperties.getWatcher().getTimeout())
                .doOnConnected(connection -> {
                    connection.addHandlerLast(new ReadTimeoutHandler(apiGatewayProperties.getWatcher().getTimeout() / 1000))
                            .addHandlerLast(new WriteTimeoutHandler(apiGatewayProperties.getWatcher().getTimeout() / 1000));
                });
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient))).build();
    }

    @Configuration
    @ConditionalOnProperty(value = "apigateway.fetcher.type", havingValue = "swagger")
    protected static class SwaggerApiFetcherConfig {
        @Bean
        public SwaggerApiFetcher swaggerApiFetcher(ApiGatewayProperties apiGatewayProperties, WebClient webClient) {
            return new SwaggerApiFetcher(apiGatewayProperties, webClient);
        }
    }

    @Configuration
    @ConditionalOnProperty(value = "apigateway.fetcher.type", havingValue = "endpoint")
    protected static class EndpointApiFetcherConfig {
        @Bean
        public EndpointApiFetcher endpointApiFetcher(ApiGatewayProperties apiGatewayProperties, WebClient webClient) {
            return new EndpointApiFetcher(apiGatewayProperties, webClient);
        }
    }

}
