package org.example.gestionuser.Services;

import lombok.AllArgsConstructor;
import org.example.gestionuser.entities.User;
import org.example.gestionuser.util.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @Override
    public void sendVerification(User user) {

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());

        String link = "http://localhost:4200/verify-email?token=" + token;

        emailService.sendVerificationEmail(user.getEmail(), link);
    }

}