package org.example.gestionuser.auth;

public interface TokenValidator {
    boolean isAuthorizationHeaderValid(String authHeader);
}

