package org.example.gestionuser.Services;

import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationEmail(String to, String link) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("maalejghada7@gmail.com");
            helper.setTo(to);
            helper.setSubject("Verify your email - GreenRoots 🌱");

            String html = """
<html>
  <body>
    <h2>Email Verification</h2>
    <p>Click below to verify your account:</p>
    <a href="%s">Verify Email</a>
  </body>
</html>
""".formatted(link);

            helper.setText(html, true);

            mailSender.send(message);

            System.out.println("✅ Email sent successfully");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Email sending failed", e);
        }
    }
}