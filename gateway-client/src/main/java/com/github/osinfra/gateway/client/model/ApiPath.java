package com.github.osinfra.gateway.client.model;

import lombok.Data;

@Data
public class ApiPath {
    /**
     * service identity from config server
     */
    private String serviceId;

    /**
     * the path of request
     */
    private String requestPath;

    /**
     * classname + methodname
     */
    private String signature;

    /**
     * need verify token or not
     * default false
     */
    private boolean requiredVerifyToken = false;

}
