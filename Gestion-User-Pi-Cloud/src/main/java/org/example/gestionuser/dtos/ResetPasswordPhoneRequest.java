package org.example.gestionuser.dtos;

import lombok.Data;

@Data
public class ResetPasswordPhoneRequest {
    private String email;
    private String telephone;
    private String code;
    private String newPassword;
}