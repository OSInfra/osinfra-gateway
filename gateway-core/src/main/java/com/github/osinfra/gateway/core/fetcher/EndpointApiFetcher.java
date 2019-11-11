package com.github.osinfra.gateway.core.fetcher;

import com.alibaba.fastjson.JSONObject;
import com.github.osinfra.gateway.core.config.ApiGatewayProperties;
import com.github.osinfra.gateway.core.model.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Objects;

/**
 * 通过获取endpoint获取暴露的api
 */
@Slf4j
public class EndpointApiFetcher implements ApiHttpFetcher {

    private ApiGatewayProperties apiGatewayProperties;

    private WebClient webClient;

    public EndpointApiFetcher(ApiGatewayProperties apiGatewayProperties, WebClient webClient) {
        this.apiGatewayProperties = apiGatewayProperties;
        this.webClient = webClient;
    }

    @Override
    public Flux<Api> apis(URI uri) {

        String url = UriComponentsBuilder.fromUriString(apiGatewayProperties.getFetcher().getRemoteApiUri())
                .port(uri.getPort())
                .host(uri.getHost())
                .scheme(uri.getScheme())
                .build()
                .toUri()
                .toString();

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(s -> Objects.requireNonNull(JSONObject.parseArray(s)).toJavaList(Api.class))
                .flatMapMany(list -> Flux.create(fluxSink -> {
                    list.forEach(it -> {
                        Api api = new Api();
                        api.setRequestPath(it.getRequestPath());
                        api.setRequiredVerifyToken(it.isRequiredVerifyToken());
                        api.setSignature(it.getSignature());
                        api.setServiceId(it.getServiceId());
                        fluxSink.next(api);
                    });
                    fluxSink.complete();
                }));

    }
}
