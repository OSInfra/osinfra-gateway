package com.github.osinfra.gateway.core.handler;

import com.github.osinfra.gateway.core.exception.ServiceNoAnyInstanceException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public class ServiceInstanceWatcher {

    private DiscoveryClient discoveryClient;
    @Getter
    private String service;
    private AtomicReference<List<ServiceInstance>> serviceInstancesReference = new AtomicReference<>(Collections.emptyList());

    public ServiceInstanceWatcher(DiscoveryClient discoveryClient, String service) {
        this.discoveryClient = discoveryClient;
        this.service = service;
    }


    public synchronized ServiceInstanceSource refresh() {
        List<ServiceInstance> instances = discoveryClient.getInstances(service);

        if (CollectionUtils.isEmpty(instances)) {
            throw new ServiceNoAnyInstanceException("No Any Instances of Service " + service, service);
        }

        // NacosServiceInstance 无equals方法
        instances = instances.stream().map(instance -> new DefaultServiceInstance(
                instance.getInstanceId(),
                instance.getServiceId(),
                instance.getHost(),
                instance.getPort(),
                instance.isSecure(),
                instance.getMetadata()
        )).collect(Collectors.toList());

        List<ServiceInstance> old = serviceInstancesReference.get();
        HashSet<ServiceInstance> oldSet = Sets.newHashSet(old);
        HashSet<ServiceInstance> newSet = Sets.newHashSet(instances);
        Set<ServiceInstance> addServices = Sets.difference(newSet, oldSet);
        Set<ServiceInstance> removeServices = Sets.difference(oldSet, newSet);
        ArrayList<ServiceInstance> addServices1 = Lists.newArrayList(addServices);
        serviceInstancesReference.set(instances);
        return new ServiceInstanceSource(addServices1, Lists.newArrayList(removeServices), service);
    }

    public synchronized void clear() {
        serviceInstancesReference.set(Collections.emptyList());
    }


    @Value
    @AllArgsConstructor
    public static class ServiceInstanceSource {
        private List<ServiceInstance> addServices;
        private List<ServiceInstance> removeServices;
        private String service;

        public boolean update() {
            return !CollectionUtils.isEmpty(addServices) || !CollectionUtils.isEmpty(removeServices);
        }
    }

}
