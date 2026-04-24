package org.example.gestioninventaire.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.gestioninventaire.dtos.response.SmsTestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwilioSmsService {

    @Value("${twilio.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.phone-number:}")
    private String fromPhoneNumber;

    @Value("${twilio.default-country-code:+216}")
    private String defaultCountryCode;

    @PostConstruct
    void init() {
        if (!smsEnabled) {
            log.info("Twilio SMS desactive (twilio.sms.enabled=false).");
            return;
        }

        if (isBlank(accountSid) || isBlank(authToken) || isBlank(fromPhoneNumber)) {
            log.warn("Twilio active mais configuration incomplete (account-sid/auth-token/phone-number).");
            return;
        }

        Twilio.init(accountSid, authToken);
        log.info("Twilio SMS initialise.");
    }

    public boolean sendSms(String toPhoneNumber, String body) {
        SmsTestResponse result = sendSmsDetailed(toPhoneNumber, body);
        return result.isSuccess();
    }

    public SmsTestResponse sendSmsDetailed(String toPhoneNumber, String body) {
        String normalizedTo = normalizePhoneNumber(toPhoneNumber);
        SmsTestResponse.SmsTestResponseBuilder response = SmsTestResponse.builder()
                .success(false)
                .smsEnabled(smsEnabled)
                .inputPhone(toPhoneNumber)
                .normalizedPhone(normalizedTo)
                .fromPhone(fromPhoneNumber);

        if (!smsEnabled) {
            return response.error("Twilio SMS desactive (twilio.sms.enabled=false).").build();
        }
        if (isBlank(accountSid) || isBlank(authToken) || isBlank(fromPhoneNumber)) {
            return response.error("Configuration Twilio incomplete (account-sid/auth-token/phone-number).").build();
        }
        if (isBlank(toPhoneNumber)) {
            return response.error("Numero destinataire vide.").build();
        }
        if (isBlank(normalizedTo)) {
            return response.error("Numero destinataire invalide apres normalisation.").build();
        }

        try {
            Message message = Message.creator(
                            new PhoneNumber(normalizedTo),
                            new PhoneNumber(fromPhoneNumber),
                            body
                    )
                    .create();
            return response.success(true).messageSid(message.getSid()).build();
        } catch (Exception e) {
            log.error("Echec envoi SMS Twilio vers {} (normalise {}): {}", toPhoneNumber, normalizedTo, e.getMessage(), e);
            return response.error(e.getMessage()).build();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizePhoneNumber(String rawPhoneNumber) {
        if (isBlank(rawPhoneNumber)) {
            return null;
        }

        String cleaned = rawPhoneNumber.trim().replaceAll("[\\s\\-()]", "");
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
        }

        if (cleaned.startsWith("+")) {
            return cleaned.matches("^\\+[1-9]\\d{6,14}$") ? cleaned : null;
        }

        if (cleaned.matches("^\\d{8}$")) {
            return defaultCountryCode + cleaned;
        }

        if (cleaned.matches("^\\d{9,15}$")) {
            return "+" + cleaned;
        }

        return null;
    }
}