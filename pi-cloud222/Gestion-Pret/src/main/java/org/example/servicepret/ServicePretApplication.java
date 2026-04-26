package org.example.servicepret;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@EnableFeignClients
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class ServicePretApplication {

    public static void main(String[] args) {
        // Charger .env manuellement
        try {
            Path envFile = Paths.get(".env");
            if (Files.exists(envFile)) {
                List<String> lines = Files.readAllLines(envFile);
                for (String line : lines) {
                    if (line.contains("=") && !line.startsWith("#")) {
                        String[] parts = line.split("=", 2);
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        System.setProperty(key, value);
                        System.out.println("Loaded: " + key);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load .env: " + e.getMessage());
        }

        SpringApplication.run(ServicePretApplication.class, args);
    }

}
