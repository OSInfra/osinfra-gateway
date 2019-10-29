package com.github.osinfra.gateway.core.handler;

import com.github.osinfra.gateway.core.config.ApiGatewayProperties;
import com.github.osinfra.gateway.core.event.RefreshApiCacheEvent;
import com.github.osinfra.gateway.core.fetcher.ApiHttpFetcher;
import com.github.osinfra.gateway.core.model.Api;
import io.netty.channel.ChannelException;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.retry.Retry;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class DiscoveryApiWatcher implements SmartLifecycle {

    private ApiRegistry apiRegistry;
    private AtomicBoolean running = new AtomicBoolean(false);

    @Resource
    private ApplicationEventPublisher publisher;

    @Resource
    private ApiGatewayProperties apiGatewayProperties;

    private PathMatcher pathMatcher = new AntPathMatcher();

    @Resource
    private ApiHttpFetcher remoteApiFetcher;

    @Resource
    private LoadBalancerClient loadBalancerClient;

    private volatile Disposable disposable;

    @Resource
    private DiscoveryServiceWatcher discoveryServiceWatcher;

    private CountDownLatch countDownLatch = new CountDownLatch(1);


    public DiscoveryApiWatcher(ApiRegistry apiRegistry) {
        this.apiRegistry = apiRegistry;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        if (running.compareAndSet(true, false)) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }
        }
        callback.run();
    }

    @Override
    public void start() {
        if (!isRunning()) {
            log.info("Initial fetch api-docs...");
            disposable = Flux.<List<ServiceInstanceWatcher.ServiceInstanceSource>>create(fluxSink -> {
                ServicesChangeListener servicesChangeListener =
                        new ServicesChangeListener() {
                            @Override
                            public void onChange(List<ServiceInstanceWatcher.ServiceInstanceSource> serviceInstanceSources) {
                                fluxSink.next(serviceInstanceSources);
                            }

                            @Override
                            public void cancel() {
                                fluxSink.complete();
                            }
                        };
                discoveryServiceWatcher.addListener(servicesChangeListener);
                discoveryServiceWatcher.start();
            }).flatMap(serviceInstanceSources ->
                    Flux.fromIterable(serviceInstanceSources)
                            .flatMap(this::fetchServiceApis)
                            .compose(registryApis())
                            .distinct()
                            .collectList()
            ).subscribe(strings -> {
                if (!CollectionUtils.isEmpty(strings)) {
                    strings = strings.stream().distinct().collect(Collectors.toList());
                    if (loadBalancerClient != null) {
                        for (String s : strings) {
                            loadBalancerClient.choose(s);
                        }
                    }
                    publish(strings);
                } else {
                    log.info("No apis of services has changed.");
                    countDownLatch.countDown();
                }
            });
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                log.error("DiscoveryApiWatcher interrupted", e);
            }
            log.info("Initial fetch api-docs done...");

        }
    }

    public void publish(List<String> strings) {

        if (!CollectionUtils.isEmpty(strings)) {
            // 首次
            if (running.compareAndSet(false, true)) {
                countDownLatch.countDown();
            }
            strings = new ArrayList<>(strings);
            log.info("Apis of services has changed, So refresh cache. Apis: {}", strings);
            publisher.publishEvent(new RefreshApiCacheEvent(this));
        }
    }

    public Flux<Api> fetchServiceApis(ServiceInstanceWatcher.ServiceInstanceSource serviceInstanceSource) {
        String serviceId = serviceInstanceSource.getService();
        return Mono.just(serviceInstanceSource)
                .compose(choseServiceFunction())
                .flatMapMany(serviceInstance ->
                        remoteApiFetcher
                                .apis(serviceInstance.getUri())
                                .map(api -> {
                                    api.setServiceId(serviceInstance.getServiceId());
                                    return api;
                                })
                                .retryWhen(Retry.anyOf(IOException.class, TimeoutException.class, ChannelException.class)
                                        .exponentialBackoff(Duration.ofMillis(apiGatewayProperties.getWatcher().getFirstBackoffMillis()), Duration.ofMillis(apiGatewayProperties.getWatcher().getMaxBackoffMills()))
                                        .retryMax(apiGatewayProperties.getWatcher().getMaxRetryTimes()))
                                .onErrorResume(throwable -> {
                                    if (throwable instanceof WebClientResponseException) {
                                        log.error("Get service apis of {} error. {}", serviceId, throwable.getMessage());
                                    } else if (throwable instanceof com.alibaba.fastjson.JSONException) {
                                        log.error("Get service apis of {} error. {}", serviceId, throwable.getMessage());
                                    } else {
                                        log.error("Get service apis of {} error. {}", serviceId, throwable.getMessage(), throwable);
                                    }
                                    return Mono.create(MonoSink::success);
                                })
                );
    }

    private Function<Mono<ServiceInstanceWatcher.ServiceInstanceSource>, Publisher<ServiceInstance>> choseServiceFunction() {
        AtomicInteger times = new AtomicInteger();
        return serviceInstanceSourceMono -> serviceInstanceSourceMono
                .filter(serviceInstanceSource -> !serviceInstanceSource.getAddServices().isEmpty())
                .map(serviceInstanceSource1 -> {
                    int curr = times.getAndIncrement();
                    if (curr >= serviceInstanceSource1.getAddServices().size()) {
                        curr = 0;
                    }
                    return serviceInstanceSource1.getAddServices().get(curr % serviceInstanceSource1.getAddServices().size());
                });
    }


    Function<Flux<Api>, Flux<String>> registryApis() {
        return o -> o.filter(api -> {
            boolean modifed = false;
            String path = api.getRequestPath();
            ApiGatewayProperties.WatcherProperties watcher = apiGatewayProperties.getWatcher();
            boolean excludedPath = matchInPatterns(path, watcher.getExcludePaths().get("default")) ||
                    (!CollectionUtils.isEmpty(watcher.getExcludePaths().get(api.getServiceId())) && matchInPatterns(path, watcher.getExcludePaths().get(api.getServiceId())));
            if (excludedPath) {
                if (apiRegistry.unregisterApi(api)) {
                    modifed = true;
                    log.debug("Remove Api {}", api);
                }
            } else {
                boolean isAdd = false;
                if (CollectionUtils.isEmpty(watcher.getIncludePaths())) {
                    isAdd = true;
                } else if (matchInPatterns(path, watcher.getIncludePaths())) {
                    isAdd = true;
                }
                if (isAdd) {
                    if (apiRegistry.registerApi(api)) {
                        modifed = true;
                        log.debug("New Api {}", api);
                    }
                }
            }
            return modifed;
        }).map(Api::getServiceId)
                .onErrorResume(throwable -> {
                    log.error("RegistryApis error", throwable);
                    return Mono.create(MonoSink::success);
                });
    }

    private boolean matchInPatterns(String path, List<String> patterns) {
        for (String p : patterns) {
            if (pathMatcher.match(p, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }
        }
    }


    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Ordered.LOWEST_PRECEDENCE - 90;
    }
}
