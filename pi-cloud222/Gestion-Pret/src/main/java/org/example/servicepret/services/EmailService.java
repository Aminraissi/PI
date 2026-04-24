package org.example.servicepret.services;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendContractEmail(String toEmail, Long contratId) {

        String link = "http://localhost:4200/loans/contrat/" + contratId;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("📄 Votre contrat de prêt est disponible");
        message.setText(
                "Bonjour,\n\n" +
                        "Votre contrat de prêt est prêt.\n" +
                        "Veuillez le signer ici :\n" +
                        link + "\n\n" +
                        "Merci."
        );

        mailSender.send(message);
    }
}
