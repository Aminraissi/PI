package tn.esprit.forums.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class UserServiceClient {

    private static final String USER_BY_ID_URL = "http://GESTION-USER/api/user/getUser/{id}";
    private static final String TOKEN_VALIDATE_URL = "http://GESTION-USER/api/auth/validate";

    private final RestTemplate restTemplate;

    public UserServiceClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void ensureUserExists(Long userId) {
        fetchUserById(userId);
    }

    public UserSummary getUserSummary(Long userId) {
        UserServiceUser response = fetchUserById(userId);
        String firstName = response.nom() == null ? "" : response.nom().trim();
        String lastName = response.prenom() == null ? "" : response.prenom().trim();
        String displayName = (firstName + " " + lastName).trim();
        if (displayName.isBlank()) {
            displayName = "Community member";
        }

        boolean isExpert = response.role() != null && response.role().toUpperCase().contains("EXPERT");
        return new UserSummary(response.id(), displayName, 0, isExpert, response.role());
    }

    public Long validateAndGetUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<TokenValidationPayload> response = restTemplate.exchange(
                    TOKEN_VALIDATE_URL,
                    HttpMethod.GET,
                    entity,
                    TokenValidationPayload.class
            );

            TokenValidationPayload payload = response.getBody();
            if (payload == null || !payload.valid() || payload.userId() == null) {
                throw new IllegalArgumentException("Invalid or expired token");
            }

            return payload.userId();
        } catch (HttpClientErrorException ex) {
            throw new IllegalArgumentException("Invalid or expired token");
        } catch (RestClientException ex) {
            throw new IllegalStateException("Could not validate token");
        }
    }

    private UserServiceUser fetchUserById(Long userId) {
        try {
            UserServiceUser response = restTemplate.getForObject(USER_BY_ID_URL, UserServiceUser.class, userId);
            if (response == null || response.id() == null) {
                throw new NoSuchElementException("User not found: " + userId);
            }
            return response;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NoSuchElementException("User not found: " + userId);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NoSuchElementException("User not found: " + userId);
            }
            throw new IllegalStateException("User service returned an error for user " + userId);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Could not reach user service");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UserServiceUser(
            Long id,
            String nom,
            String prenom,
            @JsonProperty("role") String role
    ) {
    }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record TokenValidationPayload(
            boolean valid,
            Long userId,
            String email,
            String message
        ) {
        }

    public record UserSummary(Long id, String name, int reputation, boolean isExpert, String role) {
    }
}