package tn.esprit.gateway.security;

public record UserServiceTokenValidationResponse(
        boolean valid,
        Long userId,
        String email,
        String message
) {
}
