package org.example.gestionuser.Services;

import org.example.gestionuser.dtos.UserProfileUpdateRequest;
import org.example.gestionuser.entities.StatutCompte;
import org.example.gestionuser.entities.User;

import java.util.List;

public interface IUser {
    List<User> getAllUsers();
    User updateUser (User user);
    User adduser (User user);
    User getUser (long idUser);
    void removeUser (long iduser);
    User findByEmail(String email);
    List<User> getUsersEnAttente();
    User updateStatut(Long id, StatutCompte statut);
    User updateProfile(Long id, UserProfileUpdateRequest request);
}
