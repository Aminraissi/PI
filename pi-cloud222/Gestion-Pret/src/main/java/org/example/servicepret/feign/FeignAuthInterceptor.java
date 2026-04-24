package org.example.servicepret.feign;


import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthInterceptor {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {

            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs == null) {
                System.out.println("❌ Pas de contexte HTTP (Feign appelé hors requête)");
                return;
            }

            HttpServletRequest request = attrs.getRequest();
            String token = request.getHeader("Authorization");

            if (token != null && token.startsWith("Bearer ")) {
                template.header("Authorization", token);
                System.out.println("✅ Token propagé: " + token);
            } else {
                System.out.println("❌ Token manquant ou invalide");
            }
        };
    }
}
