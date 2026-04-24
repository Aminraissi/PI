package org.example.gestioninventaire.openconfig;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .specVersion(SpecVersion.V30)  // force 3.0 au lieu de 3.1
                .info(new Info()
                        .title("Gestion Inventaire API")
                        .version("1.0.0")
                        .description("API de gestion d'inventaire"));
    }
}