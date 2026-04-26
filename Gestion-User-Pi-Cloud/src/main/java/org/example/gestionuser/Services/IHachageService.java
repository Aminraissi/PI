package org.example.gestionuser.Services;

public interface IHachageService {
    public String hashPassword(String rawPassword);
    public boolean verifyPassword(String rawPassword, String hashedPassword);
}
