package org.example.gestionuser.auth;

import org.example.gestionuser.dtos.LoginResponse;
import org.example.gestionuser.dtos.SignupResponse;
import org.example.gestionuser.dtos.SignupStep1Request;
import org.example.gestionuser.dtos.SignupStep2Request;
import org.example.gestionuser.dtos.TokenValidationResponse;

public interface AuthFacade {
    LoginResponse login(String email, String motDePasse);
    SignupResponse signupStep1(SignupStep1Request request);
    SignupResponse signupStep2(Long userId, SignupStep2Request request);
    SignupResponse verifyEmail(String token);
    TokenValidationResponse validateAuthorizationHeader(String authHeader);
}

