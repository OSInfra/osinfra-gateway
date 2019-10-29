package com.github.osinfra.gateway.core.factory;


import com.github.osinfra.gateway.core.constant.Const;
import com.github.osinfra.gateway.core.model.Api;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.server.PathContainer;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.springframework.http.server.PathContainer.parsePath;

@Slf4j
public class PathsPredicateFactory implements RoutePredicateFactory<PathsPredicateFactory.Config> {
    private PathPatternParser pathPatternParser = new PathPatternParser();


    public PathsPredicateFactory() {
    }

    @Override
    public Config newConfig() {
        return new Config();
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        Map<String, Api> pathSet = Maps.newHashMapWithExpectedSize(config.apis.size());
        Map<PathPattern, Api> pathPatterns = Maps.newHashMap();
        Map<String, PathPattern> pathToPatternMap = Maps.newHashMapWithExpectedSize(config.apis.size());
        for (Api api : config.apis) {
            try {
                if (api.getRequestPath().contains("{")) {
                    pathPatterns.put(pathPatternParser.parse(api.getRequestPath()), api);
                } else {
                    pathToPatternMap.put(api.getRequestPath(), pathPatternParser.parse(api.getRequestPath()));
                    pathSet.put(api.getRequestPath(), api);
                }
            } catch (Exception e) {
                log.warn("解析错误{} ", api.getRequestPath(), e);
            }
        }
        return exchange -> {
            String path = exchange.getRequest().getURI().getPath();
            if (pathSet.containsKey(path)) {
                exchange.getAttributes().put(Const.ExchangeAttribute.API_VARIABLES_ATTRIBUTE, pathSet.get(path));
                exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, pathToPatternMap.get(path));
                return true;
            }
            PathContainer container = parsePath(path);
            for (PathPattern pathPattern : pathPatterns.keySet()) {
                if (pathPattern.matches(container)) {
                    exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, pathPattern);
                    exchange.getAttributes().put(Const.ExchangeAttribute.API_VARIABLES_ATTRIBUTE, pathPatterns.get(pathPattern));
                    return true;
                }
            }
            return false;
        };
    }

    @Validated
    public static class Config {

        private List<Api> apis;

        public PathsPredicateFactory.Config setApis(List<Api> apis) {
            this.apis = apis;
            return this;
        }

        @Override
        public String toString() {
            return new ToStringCreator(this)
                    .append("apis", apis)
                    .toString();
        }
    }


}
