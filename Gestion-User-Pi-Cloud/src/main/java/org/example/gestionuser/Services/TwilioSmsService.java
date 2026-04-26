package org.example.gestionuser.Services;

import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwilioSmsService implements SmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.verify-service-sid:}")
    private String verifyServiceSid;

    private boolean twilioReady = false;

    @PostConstruct
    public void initTwilio() {
        if (accountSid == null || accountSid.isBlank()
                || authToken == null || authToken.isBlank()
                || verifyServiceSid == null || verifyServiceSid.isBlank()) {

            System.out.println("Twilio Verify configuration missing. OTP sending is disabled.");
            return;
        }

        Twilio.init(accountSid, authToken);
        twilioReady = true;

        System.out.println("Twilio Verify initialized successfully.");
    }

    @Override
    public void sendSms(String toPhoneNumber, String messageBody) {
        if (!twilioReady) {
            System.out.println("========== SMS DEMO ==========");
            System.out.println("To: " + toPhoneNumber);
            System.out.println("Message: " + messageBody);
            System.out.println("==============================");
            return;
        }

        Verification.creator(
                verifyServiceSid,
                toPhoneNumber,
                "sms"
        ).create();
    }

    public boolean checkCode(String toPhoneNumber, String code) {
        if (!twilioReady) {
            System.out.println("Twilio Verify not configured. Demo accepts code 123456.");
            return "123456".equals(code);
        }

        VerificationCheck verificationCheck = VerificationCheck.creator(verifyServiceSid)
                .setTo(toPhoneNumber)
                .setCode(code)
                .create();

        return "approved".equalsIgnoreCase(verificationCheck.getStatus());
    }
}