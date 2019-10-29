package com.github.osinfra.gateway.core.handler;

import com.github.osinfra.gateway.core.config.ApiGatewayProperties;
import com.github.osinfra.gateway.core.exception.ServiceNoAnyInstanceException;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.CollectionUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.retry.Retry;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class DiscoveryServiceWatcher implements SmartLifecycle {

    private DiscoveryClient discoveryClient;
    private Map<String, ServiceInstanceWatcher> serviceInstanceWatcherTable = new HashMap<>();
    private ServicesRefreshListener servicesRefreshListener;
    private List<ServicesChangeListener> servicesChangeListeners = Lists.newArrayList();
    private final TaskScheduler taskScheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> watchFuture;
    private Disposable disposater;

    @Resource
    private ApiRegistry apiRegistry;

    @Resource
    private ApiGatewayProperties apiGatewayProperties;

    @Resource
    private DiscoveryApiWatcher discoveryApiWatcher;

    public DiscoveryServiceWatcher(DiscoveryClient discoveryClient) {
        this.taskScheduler = getTaskScheduler();
        this.discoveryClient = discoveryClient;
    }

    private static ThreadPoolTaskScheduler getTaskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        return taskScheduler;
    }

    public synchronized void clear() {
        serviceInstanceWatcherTable.clear();
    }

    @Override
    public void start() {
        if (this.running.compareAndSet(false, true)) {
            disposater = Flux.<List<ServiceInstanceWatcher>>create(listMonoSink -> servicesRefreshListener = new ServicesRefreshListener() {
                @Override
                public void onRefresh(List<String> services) {
                    List<ServiceInstanceWatcher> serviceInstanceWatchers = Lists.newArrayList();
                    synchronized (this) {
                        if (CollectionUtils.isEmpty(services)) {
                            serviceInstanceWatcherTable.clear();
                        } else {
                            for (ServiceInstanceWatcher serviceInstanceWatcher : serviceInstanceWatchers) {
                                if (!services.contains(serviceInstanceWatcher.getService())) {
                                    serviceInstanceWatcherTable.remove(serviceInstanceWatcher.getService());
                                }
                            }
                            for (String service : services) {
                                ServiceInstanceWatcher instanceWatcher = serviceInstanceWatcherTable.get(service);
                                if (instanceWatcher == null) {
                                    instanceWatcher = new ServiceInstanceWatcher(discoveryClient, service);
                                    serviceInstanceWatcherTable.put(service, instanceWatcher);
                                }
                                serviceInstanceWatchers.add(instanceWatcher);
                            }
                        }
                    }
                    if (!CollectionUtils.isEmpty(serviceInstanceWatchers)) {
                        listMonoSink.next(serviceInstanceWatchers);
                    }
                }

                @Override
                public void cancel() {
                    listMonoSink.complete();
                }

            }).flatMap(serviceInstanceWatchers ->
                    Flux.fromIterable(serviceInstanceWatchers)
                            .flatMap(serviceInstanceSource -> Mono
                                    .defer(() -> {
                                        ServiceInstanceWatcher.ServiceInstanceSource refresh = serviceInstanceSource.refresh();
                                        return Mono.just(refresh);
                                    })
                                    .retryWhen(Retry
                                            .anyOf(IOException.class, ServiceNoAnyInstanceException.class, TimeoutException.class)
                                            .exponentialBackoff(Duration.ofMillis(apiGatewayProperties.getWatcher().getFirstBackoffMillis()), Duration.ofMillis(apiGatewayProperties.getWatcher().getMaxBackoffMills()))
                                            .retryMax(apiGatewayProperties.getWatcher().getMaxRetryTimes()))
                                    .onErrorResume(throwable -> {
                                        log.warn("Watch service {} error ... error: {}", serviceInstanceSource.getService(), throwable.getMessage());
                                        return Mono.create(MonoSink::success);
                                    }))
                            .filter(ServiceInstanceWatcher.ServiceInstanceSource::update)
                            .collectList()
            ).subscribe(serviceInstanceSources -> {
                if (!CollectionUtils.isEmpty(serviceInstanceSources)) {
                    log.info("ServiceInstances changed. Services: {}", serviceInstanceSources);
                    for (ServicesChangeListener servicesChangeListener : servicesChangeListeners) {
                        servicesChangeListener.onChange(serviceInstanceSources);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("ServiceInstances No changed");
                    }
                }
            });
            this.refresh();
            this.watchFuture = this.taskScheduler.scheduleWithFixedDelay(this::refresh,
                    5000);
        }

    }


    @Override
    public void stop() {
        if (this.running.compareAndSet(true, false) && this.watchFuture != null) {
            this.watchFuture.cancel(true);
            disposater.dispose();
        }
    }

    @Override
    public boolean isRunning() {
        return this.running.get();
    }

    private void refresh() {
        refreshService();
    }

    private void refreshService() {
        List<String> services = discoveryClient.getServices().stream()
                .filter(s -> !apiGatewayProperties.getWatcher()
                        .getExcludeServiceIds()
                        .contains(s))
                .distinct()
                .collect(Collectors.toList());
        if (log.isDebugEnabled()) {
            log.debug("api gateway refresh services: {}", services);
        }
        servicesRefreshListener.onRefresh(services);
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable runnable) {
        if (this.running.compareAndSet(true, false) && this.watchFuture != null) {
            this.watchFuture.cancel(true);
            disposater.dispose();
        }
        runnable.run();
    }

    @Override
    public int getPhase() {
        return 0;
    }

    public void addListener(ServicesChangeListener servicesChangeListener) {
        this.servicesChangeListeners.add(servicesChangeListener);
    }

    private interface ServicesRefreshListener {

        void onRefresh(List<String> services);

        void cancel();
    }


}
