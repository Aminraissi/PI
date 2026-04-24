package org.exemple.farmersupport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FarmerSupportApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarmerSupportApplication.class, args);
    }

}
