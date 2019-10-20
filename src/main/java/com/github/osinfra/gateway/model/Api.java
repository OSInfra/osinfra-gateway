package com.github.osinfra.gateway.model;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Api {

    /**
     * service identity from config server
     */
    private String serviceId;

    /**
     * the path of request
     */
    private String requestPath;

    /**
     * need verify token or not
     * default false
     */
    private boolean requiredVerifyToken = false;
}
