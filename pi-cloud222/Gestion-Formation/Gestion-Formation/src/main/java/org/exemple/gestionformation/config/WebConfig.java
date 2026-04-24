package org.exemple.gestionformation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    @Value("${app.video-upload.dir:uploads/videos}")
    private String videoUploadDir;

    @Value("${app.resource-upload.dir:uploads/resources}")
    private String resourceUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(uploadPath.toUri().toString());

        Path videoUploadPath = Paths.get(videoUploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/videos/**")
                .addResourceLocations(videoUploadPath.toUri().toString());

        Path resourceUploadPath = Paths.get(resourceUploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/resources/**")
                .addResourceLocations(resourceUploadPath.toUri().toString());
    }
}
