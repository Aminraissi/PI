package org.example.gestionuser.auth;

import lombok.AllArgsConstructor;
import org.example.gestionuser.util.JwtTokenProvider;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class JwtTokenValidator implements TokenValidator {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean isAuthorizationHeaderValid(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        String token = authHeader.substring(7);
        return jwtTokenProvider.validateToken(token);
    }
}

