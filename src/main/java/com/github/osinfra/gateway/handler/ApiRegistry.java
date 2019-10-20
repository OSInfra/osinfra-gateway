package com.github.osinfra.gateway.handler;


import com.github.osinfra.gateway.model.Api;
import com.google.common.collect.*;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ApiRegistry {

    private final Table<String, String, Api> apiTable = HashBasedTable.create();

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private Lock readLock = readWriteLock.readLock();

    private Lock writeLock = readWriteLock.writeLock();

    /**
     * register api
     */
    public boolean registerApi(Api api) {
        Assert.notNull(api, "api cannot be null when register");

        String serviceId = api.getServiceId();
        String requestPath = api.getRequestPath();

        try {
            writeLock.lock();

            Api cacheApi = apiTable.get(serviceId, requestPath);

            if (Objects.isNull(cacheApi) || !api.equals(cacheApi)) {

                apiTable.put(serviceId, requestPath, api);

                return true;
            }
            return false;
        } finally {

            writeLock.unlock();

        }
    }

    public ImmutableListMultimap<String, Api> groupByServiceId() {

        try {

            readLock.lock();

            return Multimaps.index(apiTable.values(), Api::getServiceId);

        } finally {

            readLock.unlock();

        }
    }

    public List<Api> apis() {
        try {

            readLock.lock();

            return ImmutableList.copyOf(apiTable.values());

        } finally {

            readLock.unlock();

        }
    }


    public boolean unregisterApi(Api api) {
        Assert.notNull(api, "api cannot be null when register");

        try {
            writeLock.lock();

            return Objects.nonNull(apiTable.remove(api.getServiceId(), api.getRequestPath()));

        } finally {

            writeLock.unlock();

        }
    }

    public void clear() {

        try {

            writeLock.lock();

            apiTable.clear();

        } finally {

            writeLock.unlock();

        }
    }
}
