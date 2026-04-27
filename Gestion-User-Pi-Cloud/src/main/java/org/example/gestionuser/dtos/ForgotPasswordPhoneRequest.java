package org.example.gestionuser.dtos;

import lombok.Data;

@Data
public class ForgotPasswordPhoneRequest {
    private String email;
    private String telephone;
}