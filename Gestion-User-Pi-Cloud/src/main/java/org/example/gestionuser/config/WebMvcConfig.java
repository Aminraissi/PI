package org.example.gestionuser.config;

import lombok.AllArgsConstructor;
import org.example.gestionuser.Services.LocalUserFileStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@AllArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final LocalUserFileStorageService localUserFileStorageService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
registry.addInterceptor(jwtAuthInterceptor)
        .excludePathPatterns(
            "/api/auth/**"
        );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(localUserFileStorageService.getUploadRootLocation());
    }
}
