package org.example.gestionuser.Services;

import org.example.gestionuser.entities.User;

public interface EmailService {
    void sendVerificationEmail(String to, String link);

}
