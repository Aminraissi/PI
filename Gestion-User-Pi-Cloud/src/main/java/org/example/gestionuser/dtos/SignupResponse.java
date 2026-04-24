package org.example.gestionuser.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {
    private Long userId;
    private String email;
    private String role;
    private String statutCompte;
    private String emailVerificationStatus;
    private String profileValidationStatus;
    private String nextStep;
    private String message;
}
