package com.github.osinfra.gateway.example;

import com.github.osinfra.gateway.client.annotation.AuthorizeToken;
import com.github.osinfra.gateway.client.annotation.IgnoreGateway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@EnableDiscoveryClient
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


    @RestController
    @RequestMapping("/api/aaa")
    class A {

        @RequestMapping("/test1")
        @AuthorizeToken
        public String test(){
            return "test1";
        }

        @RequestMapping("/test2")
//        @IgnoreGateway
        public String test2(){
            return "test2";
        }

    }
}
