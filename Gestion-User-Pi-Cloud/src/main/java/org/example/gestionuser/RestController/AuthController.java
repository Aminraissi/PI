package org.example.gestionuser.RestController;

import lombok.AllArgsConstructor;
import org.example.gestionuser.auth.AuthFacade;
import org.example.gestionuser.dtos.LoginRequest;
import org.example.gestionuser.dtos.LoginResponse;
import org.example.gestionuser.dtos.SignupResponse;
import org.example.gestionuser.dtos.SignupStep1Request;
import org.example.gestionuser.dtos.SignupStep2Request;
import org.example.gestionuser.dtos.TokenValidationResponse;
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
        LoginResponse response = authFacade.login(request.getEmail(), request.getMotDePasse());

        // if (response.getToken() == null) {
        //     if (response.isVerificationRequired()) {
        //         return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        //     }
        //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        // }
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

    @PostMapping("/verify-email/{userId}")
    public ResponseEntity<?> verifyEmail(@PathVariable Long userId) {
        SignupResponse response = authFacade.verifyEmail(userId);
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
}
