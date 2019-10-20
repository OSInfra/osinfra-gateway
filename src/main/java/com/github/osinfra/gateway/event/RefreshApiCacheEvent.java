package com.github.osinfra.gateway.event;

import org.springframework.context.ApplicationEvent;

public class RefreshApiCacheEvent extends ApplicationEvent{

    public RefreshApiCacheEvent(Object source) {
        super(source);
    }
}
