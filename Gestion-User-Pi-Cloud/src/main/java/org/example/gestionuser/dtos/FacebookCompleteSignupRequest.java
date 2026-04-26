package org.example.gestionuser.dtos;

import lombok.Data;

@Data
public class FacebookCompleteSignupRequest {
    private String accessToken;
    private String telephone;
    private String role;
}