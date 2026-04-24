package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SmsTestResponse {
    private boolean success;
    private boolean smsEnabled;
    private String inputPhone;
    private String normalizedPhone;
    private String fromPhone;
    private String messageSid;
    private String error;
}