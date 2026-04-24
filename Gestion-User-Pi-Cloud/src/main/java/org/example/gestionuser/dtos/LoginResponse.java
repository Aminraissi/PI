package org.example.gestionuser.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String email;
    private String role;
    private String statutCompte;
    private String emailVerificationStatus;
    private String profileValidationStatus;
    private String nextStep;
    private boolean verificationRequired;
    private String message;
}

