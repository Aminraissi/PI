package org.example.gestionuser.dtos;

import lombok.Data;

@Data
public class GoogleCompleteSignupRequest {
    private String credential;
    private String telephone;
    private String role;
}