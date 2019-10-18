package com.github.osinfra.gateway.web;

import com.google.common.collect.Lists;
import org.springframework.cloud.alibaba.nacos.NacosDiscoveryClient;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/osinfra/gateway")
public class ServiceController {

    @Resource
    private NacosDiscoveryClient nacosDiscoveryClient;

    @GetMapping("/fetch-service")
    public Mono<List<ServiceInstance>> fetchAllServices() {
        List<String> serviceNames = nacosDiscoveryClient.getServices();

        List<ServiceInstance> serviceInstances = Lists.newArrayList();
        serviceNames.forEach(it -> {
            List<ServiceInstance> cur = nacosDiscoveryClient.getInstances(it);
            serviceInstances.addAll(cur);
        });
        return Mono.just(serviceInstances);
    }
}
