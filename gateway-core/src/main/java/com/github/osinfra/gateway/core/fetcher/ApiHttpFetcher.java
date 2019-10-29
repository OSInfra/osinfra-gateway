package com.github.osinfra.gateway.core.fetcher;

import com.github.osinfra.gateway.core.model.Api;
import reactor.core.publisher.Flux;

import java.net.URI;

public interface ApiHttpFetcher extends ApiFetcher {

    Flux<Api> apis(URI uri);

}
