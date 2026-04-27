package org.example.gestionuser.Services;

import org.example.gestionuser.entities.User;

public interface EmailVerificationService {
    void sendVerification(User user);
}
