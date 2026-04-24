package org.example.gestionvente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableJpaRepositories(basePackages = "org.example.gestionvente.Repositories")
public class GestionVenteApplication {
    public static void main(String[] args) {
        SpringApplication.run(GestionVenteApplication.class, args);
    }
}