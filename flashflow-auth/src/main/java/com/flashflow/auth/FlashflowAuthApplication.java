package com.flashflow.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.flashflow")
@EnableDiscoveryClient
public class FlashflowAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashflowAuthApplication.class, args);
    }
}
