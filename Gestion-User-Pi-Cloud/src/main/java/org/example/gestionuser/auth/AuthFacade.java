package org.example.gestionuser.auth;

import org.example.gestionuser.dtos.*;

public interface AuthFacade {
    LoginResponse login(String email, String motDePasse, String captchaToken);
    SignupResponse signupStep1(SignupStep1Request request);
    SignupResponse signupStep2(Long userId, SignupStep2Request request);
    SignupResponse verifyEmail(String token);
    SignupResponse forgotPasswordCheckEmail(String email);
    SignupResponse forgotPasswordByPhone(String email, String telephone);
    SignupResponse resetPasswordByPhone(String email, String telephone, String code, String newPassword);
    LoginResponse loginWithGoogle(String credential);
    LoginResponse completeGoogleSignup(GoogleCompleteSignupRequest request);
    LoginResponse completeFacebookSignup(FacebookCompleteSignupRequest request);
    LoginResponse loginWithFacebook(String accessToken);
    TokenValidationResponse validateAuthorizationHeader(String authHeader);
}