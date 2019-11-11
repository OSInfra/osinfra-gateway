package com.github.osinfra.gateway.core.config;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.support.TimeoutException;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@Data
@ConfigurationProperties("apigateway")
public class ApiGatewayProperties {

    private WatcherProperties watcher = new WatcherProperties();

    private FetcherProperties fetcher = new FetcherProperties();

    private RetryerProperties retryer = new RetryerProperties();

    private FilterProperties filter = new FilterProperties();

    private AuthorizeProperties authorize = new AuthorizeProperties();

    @Data
    public static class FilterProperties {
        private List<String> requestHeaders = Lists.newArrayList();
    }

    @Data
    public static class FetcherProperties {
        private String type = "swagger";
        private String remoteApiUri;
    }

    @Data
    public static class WatcherProperties {
        private boolean loadOnStart;
        private int refreshPeriod = 1000;
        private Integer timeout = 5000;
        private List<String> includePaths = Lists.newArrayList();
        private Map<String, List<String>> excludePaths = Maps.newHashMap();
        private List<String> excludeServiceIds = Lists.newArrayList();
        private Integer firstBackoffMillis = 500;
        private Integer maxBackoffMills = 3000;
        private Integer maxRetryTimes = 3;

        public Integer getMaxRetryTimes() {
            return maxRetryTimes;
        }

        public void setMaxRetryTimes(Integer maxRetryTimes) {
            this.maxRetryTimes = maxRetryTimes;
        }
    }

    @Data
    public static class RetryerProperties {
        private List<Class> exceptions = Lists.newArrayList(IOException.class, TimeoutException.class);
        private int retries = 3;
    }

    @Data
    public static class AuthorizeProperties {
        private boolean enabled = false;
    }

}