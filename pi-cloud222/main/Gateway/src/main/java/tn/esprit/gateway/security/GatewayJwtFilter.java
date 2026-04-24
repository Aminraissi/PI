package tn.esprit.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GatewayJwtFilter extends OncePerRequestFilter {

    private static final String USER_VALIDATE_URL = "http://GESTION-USER/api/auth/validate";

    private static final Set<String> PUBLIC_AUTH_PREFIXES = Set.of(
            "/user/api/auth/login",
            "/user/api/auth/signup",
            "/user/api/auth/verify-email",
            "/user/api/auth/validate"
    );

    private final RestTemplate restTemplate;

    public GatewayJwtFilter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        if (path.startsWith("/actuator")) {
            return true;
        }

        if (PUBLIC_AUTH_PREFIXES.stream().anyMatch(path::startsWith)) {
            return true;
        }

        // Public read access for forums; protect only write operations.
        // TODO: Make this path check service-agnostic so other microservices can be protected here too.
        return !path.startsWith("/forums/") || "GET".equalsIgnoreCase(method);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<UserServiceTokenValidationResponse> validationResponse = restTemplate.exchange(
                    USER_VALIDATE_URL,
                    HttpMethod.GET,
                    entity,
                    UserServiceTokenValidationResponse.class
            );

            UserServiceTokenValidationResponse body = validationResponse.getBody();
            if (body == null || !body.valid() || body.userId() == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is invalid or expired");
                return;
            }
        } catch (RestClientException ex) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Could not validate token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
