package com.github.osinfra.gateway.fetcher;

import com.github.osinfra.gateway.model.Api;
import reactor.core.publisher.Flux;

import java.net.URI;

public interface ApiHttpFetcher extends ApiFetcher {

    Flux<Api> apis(URI uri);

}
