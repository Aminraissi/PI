package org.example.gestionuser.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecaptchaServiceImpl implements IRecaptchaService {

    @Value("${recaptcha.secret-key}")
    private String secretKey;

    @Value("${recaptcha.verify-url}")
    private String verifyUrl;

    @Value("${recaptcha.enabled:true}")
    private boolean recaptchaEnabled;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verifyCaptcha(String captchaResponse) {
        // Mode développement : désactiver CAPTCHA
        if (!recaptchaEnabled) {
            System.out.println(" CAPTCHA désactivé (mode développement)");
            return true;
        }
        if ("dev-simulated-token".equals(captchaResponse)) {
            System.out.println(" Token simulé accepté (mode développement)");
            return true;
        }

        if (captchaResponse == null || captchaResponse.isBlank()) {
            System.err.println(" CAPTCHA: Token reçu est null ou vide");
            return false;
        }

        System.out.println("Vérification CAPTCHA v2 - Token: " + captchaResponse.substring(0, Math.min(30, captchaResponse.length())) + "...");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "secret=" + secretKey + "&response=" + captchaResponse;
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(verifyUrl, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            System.out.println(" Réponse Google: " + responseBody);

            if (responseBody != null) {
                boolean success = (boolean) responseBody.getOrDefault("success", false);
                String errorCodes = (String) responseBody.getOrDefault("error-codes", "[]");

                System.out.println("   Success: " + success);
                System.out.println("   Error codes: " + errorCodes);

                return success;
            }
        } catch (Exception e) {
            System.err.println(" CAPTCHA verification failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}