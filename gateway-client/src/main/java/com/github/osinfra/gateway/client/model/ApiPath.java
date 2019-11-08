package com.github.osinfra.gateway.client.model;

import lombok.Data;

@Data
public class ApiPath {

    private String url;

    // classname + methodname
    private String signature;

    private boolean needAuthorize;

}
