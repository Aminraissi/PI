package org.example.gestionuser.RestController;

import lombok.AllArgsConstructor;
import org.example.gestionuser.auth.AuthFacade;
import org.example.gestionuser.dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthFacade authFacade;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        LoginResponse response = authFacade.login(request.getEmail(), request.getMotDePasse(), request.getCaptchaToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup/step1")
    public ResponseEntity<?> signupStep1(@RequestBody SignupStep1Request request) {
        SignupResponse response = authFacade.signupStep1(request);
        if (response.getUserId() == null) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/signup/step2/{userId}")
    public ResponseEntity<?> signupStep2(@PathVariable Long userId, @RequestBody SignupStep2Request request) {
        SignupResponse response = authFacade.signupStep2(userId, request);
        if (response.getUserId() == null) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        SignupResponse response = authFacade.verifyEmail(token);
        if (response.getUserId() == null) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenValidationResponse response = authFacade.validateAuthorizationHeader(authHeader);
        if (!response.isValid()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/email")
    public ResponseEntity<?> forgotPasswordCheckEmail(@RequestBody ForgotPasswordEmailRequest request) {
        SignupResponse response = authFacade.forgotPasswordCheckEmail(request.getEmail());
        if (response.getUserId() == null) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/phone")
    public ResponseEntity<?> forgotPasswordByPhone(@RequestBody ForgotPasswordPhoneRequest request) {
        SignupResponse response = authFacade.forgotPasswordByPhone(
                request.getEmail(),
                request.getTelephone()
        );
        if (response.getUserId() == null) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password/phone")
    public ResponseEntity<?> resetPasswordByPhone(@RequestBody ResetPasswordPhoneRequest request) {
        SignupResponse response = authFacade.resetPasswordByPhone(
                request.getEmail(),
                request.getTelephone(),
                request.getCode(),
                request.getNewPassword()
        );
        if (response.getUserId() == null) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        LoginResponse response = authFacade.loginWithGoogle(request.getCredential());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google/complete-signup")
    public ResponseEntity<?> completeGoogleSignup(@RequestBody GoogleCompleteSignupRequest request) {
        LoginResponse response = authFacade.completeGoogleSignup(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/facebook/complete-signup")
    public ResponseEntity<?> completeFacebookSignup(@RequestBody FacebookCompleteSignupRequest request) {
        LoginResponse response = authFacade.completeFacebookSignup(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/facebook")
    public ResponseEntity<?> loginWithFacebook(@RequestBody FacebookLoginRequest request) {
        LoginResponse response = authFacade.loginWithFacebook(request.getAccessToken());
        return ResponseEntity.ok(response);
    }
}