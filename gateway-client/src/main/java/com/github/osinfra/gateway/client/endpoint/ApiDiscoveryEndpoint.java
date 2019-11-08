package com.github.osinfra.gateway.client.endpoint;


import com.github.osinfra.gateway.client.annotation.AuthorizeToken;
import com.github.osinfra.gateway.client.annotation.IgnoreGateway;
import com.github.osinfra.gateway.client.configuration.GatewayClientProperty;
import com.github.osinfra.gateway.client.model.ApiPath;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Endpoint(id = "api-gateway")
public class ApiDiscoveryEndpoint {

    @Resource
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private GatewayClientProperty gatewayClientProperty;

    public ApiDiscoveryEndpoint(GatewayClientProperty gatewayClientProperty) {
        this.gatewayClientProperty = gatewayClientProperty;
    }

    /**
     * 暴露需要接网关的接口
     */
    @ReadOperation
    public List<ApiPath> apiPathCollector() {

        // 获取url与类和方法的对应信息
        Map<RequestMappingInfo, HandlerMethod> mappingInfos = requestMappingHandlerMapping.getHandlerMethods();

        // api信息
        List<ApiPath> apiPaths = Lists.newArrayList();

        String excludePathStr = Joiner.on("#").join(gatewayClientProperty.getExcludePathPrefix()).concat("#");

        for (Map.Entry<RequestMappingInfo, HandlerMethod> current : mappingInfos.entrySet()) {
            RequestMappingInfo requestMappingInfo = current.getKey();
            HandlerMethod handlerMethod = current.getValue();
            PatternsRequestCondition patternsCondition = requestMappingInfo.getPatternsCondition();
            Method method = handlerMethod.getMethod();

            // classname + method
            String signature = method.getDeclaringClass().getName() + "#" + method.getName();

            // 排除IgnoreGateway的path
            if (method.isAnnotationPresent(IgnoreGateway.class)
                    || method.getDeclaringClass().isAnnotationPresent(IgnoreGateway.class)) {
                continue;
            }

            boolean needAuthorize = method.isAnnotationPresent(AuthorizeToken.class)
                    || method.getDeclaringClass().isAnnotationPresent(AuthorizeToken.class);

            for (String url : patternsCondition.getPatterns()) {

                if (!StringUtils.isEmpty(excludePathStr) && excludePathStr.contains(url + "#")) {
                    continue;
                }

                ApiPath apiPath = new ApiPath();
                apiPath.setUrl(url);
                apiPath.setSignature(signature);
                apiPath.setNeedAuthorize(needAuthorize);
                apiPaths.add(apiPath);

            }
        }

        return apiPaths;
    }

}
