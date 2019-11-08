package com.github.osinfra.gateway.client.configuration;


import com.google.common.collect.Lists;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "osinfra.gateway.client")
@Data
public class GatewayClientProperty {

    private List<String> excludePathPrefix = Lists.newArrayList();

}
