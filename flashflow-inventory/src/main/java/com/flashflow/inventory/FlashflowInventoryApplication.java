package com.flashflow.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.flashflow")
@EnableDiscoveryClient
public class FlashflowInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashflowInventoryApplication.class, args);
    }
}
