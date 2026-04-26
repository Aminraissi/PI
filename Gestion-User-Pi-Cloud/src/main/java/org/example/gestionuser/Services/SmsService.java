package org.example.gestionuser.Services;

public interface SmsService {
    void sendSms(String toPhoneNumber, String message);
    boolean checkCode(String toPhoneNumber, String code);
}