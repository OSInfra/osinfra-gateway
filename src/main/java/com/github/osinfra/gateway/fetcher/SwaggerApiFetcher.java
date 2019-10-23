package com.github.osinfra.gateway.fetcher;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.github.osinfra.gateway.configuration.ApiGatewayProperties;
import com.github.osinfra.gateway.model.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;

@Slf4j
public class SwaggerApiFetcher implements ApiHttpFetcher {

    public static final String JSON_KEY_PATHS_TOKEN_KEY = "Authorization";
    public static final String JSON_KEY_PATHS = "paths";
    public static final String JSON_KEY_PATHS_SECURITY = "security";
    private ApiGatewayProperties properties;
    private WebClient webClient;


    public SwaggerApiFetcher(WebClient webClient, ApiGatewayProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @Override
    public Flux<Api> apis(URI uri) {
        uri = UriComponentsBuilder.fromUriString(properties.getFetcher().getRemoteApiUri()).port(uri.getPort()).host(uri.getHost()).scheme(uri.getScheme()).build().toUri();
        String url = uri.toString();
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(s -> JSONObject.parseObject(s, Feature.DisableCircularReferenceDetect))
                .filter(jsonObject -> jsonObject != null && jsonObject.getJSONObject(JSON_KEY_PATHS) != null)
                .flatMapMany(jsonObject -> {
                    return Flux.create(fluxSink -> {
                        JSONObject paths = jsonObject.getJSONObject(JSON_KEY_PATHS);

                        for (String path : paths.keySet()) {
                            Api api = new Api();
                            api.setRequestPath(path);
                            api.setRequiredVerifyToken(getRequiredVerifyToken(paths.getJSONObject(path)));
                            fluxSink.next(api);
                        }
                        fluxSink.complete();
                    });
                });
    }

    private boolean getRequiredVerifyToken(JSONObject pathJsonObject) {
        if (pathJsonObject == null) {
            return false;
        }
        for (String s : pathJsonObject.keySet()) {
            JSONArray security = pathJsonObject.getJSONObject(s).getJSONArray(JSON_KEY_PATHS_SECURITY);
            if (security != null) {
                for (int i = 0; i < security.size(); i++) {
                    if (security.getJSONObject(i).keySet().contains(JSON_KEY_PATHS_TOKEN_KEY)) {
                        return true;
                    }
                }
            }
            break;
        }
        return false;

    }

}
