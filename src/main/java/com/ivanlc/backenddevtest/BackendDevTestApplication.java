package com.ivanlc.backenddevtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendDevTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendDevTestApplication.class, args);
    }
}
