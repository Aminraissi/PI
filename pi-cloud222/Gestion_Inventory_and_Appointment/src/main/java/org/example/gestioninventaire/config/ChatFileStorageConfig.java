package org.example.gestioninventaire.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class ChatFileStorageConfig implements WebMvcConfigurer {

    @Value("${app.chat.upload-dir:chat-uploads}")
    private String chatUploadDir;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String chatLocation = Paths.get(chatUploadDir).toAbsolutePath().normalize().toUri().toString();
        String uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/chat-uploads/**")
                .addResourceLocations(chatLocation);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }
}
