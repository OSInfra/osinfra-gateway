package com.github.osinfra.gateway.configuration;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.github.osinfra.gateway.handler.ApiRegistry;
import com.github.osinfra.gateway.handler.DiscoveryApiWatcher;
import com.github.osinfra.gateway.handler.DiscoveryServiceWatcher;
import com.github.osinfra.gateway.locator.ApiCacheRouterLocator;
import com.github.osinfra.gateway.locator.ApiRouteLocator;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.alibaba.nacos.NacosDiscoveryClient;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class ApiGateWayAutoConfiguration {


    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public ApiGateWayAutoConfiguration(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                                       ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    @Bean
    public ApiRegistry apiRegistry() {
        return new ApiRegistry();
    }

    @Bean
    public ApiCacheRouterLocator apiRouteLocator(ListableBeanFactory beanFactory) {
        ApiRouteLocator apiRouteLocator = new ApiRouteLocator(beanFactory);
        return new ApiCacheRouterLocator(apiRouteLocator);
    }

    @Bean
    public DiscoveryApiWatcher apiReloader(ApiRegistry apiRegistry) {
        return new DiscoveryApiWatcher(apiRegistry);
    }

    @Bean
    public DiscoveryServiceWatcher discoveryServiceWatcher(DiscoveryClient discoveryClient) {
        return new DiscoveryServiceWatcher(discoveryClient);
    }

    @Bean
    @Order(-1)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        // Register the block exception handler for Spring Cloud Gateway.
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    @Bean
    public GlobalFilter sentinelGatewayFilter() {
        // By default the order is HIGHEST_PRECEDENCE
        return new SentinelGatewayFilter();
    }

    @PostConstruct
    public void doInit() {
//        initCustomizedApis();
        initGatewayRules();
    }

//    private void initCustomizedApis() {
//        Set<ApiDefinition> definitions = new HashSet<>();
//        ApiDefinition api1 = new ApiDefinition("some_customized_api")
//                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
//                    add(new ApiPathPredicateItem().setPattern("/ahas"));
//                    add(new ApiPathPredicateItem().setPattern("/product/**")
//                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
//                }});
//        ApiDefinition api2 = new ApiDefinition("another_customized_api")
//                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
//                    add(new ApiPathPredicateItem().setPattern("/**")
//                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
//                }});
//        definitions.add(api1);
//        definitions.add(api2);
//        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
//    }

    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule("osinfra-user")
                .setCount(1)
                .setIntervalSec(1)
        );
        GatewayRuleManager.loadRules(rules);
    }
}
