package org.example.gestionuser.Services;

import lombok.AllArgsConstructor;
import org.example.gestionuser.Repositories.UserRepo;

import org.example.gestionuser.entities.Role;

import org.example.gestionuser.entities.ProfileValidationStatus;

import org.example.gestionuser.entities.StatutCompte;
import org.example.gestionuser.entities.User;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@AllArgsConstructor
public class UserServiceImp implements IUser{
    private UserRepo ur;
    @Override
    public List<User> getAllUsers() {
        return ur.findAll();
    }

    @Override
    public User updateUser(User user) {
        return ur.save(user);
    }
    @Override
    public User adduser(User user) {
        return ur.save(user);
    }

    @Override
    public User getUser (long iduser) {
        return ur.findById(iduser).orElse(null);
    }
    @Override
    public void removeUser(long iduser) {
        ur.deleteById(iduser);

    }

    @Override
    public User findByEmail(String email) {
        return ur.findByEmail(email).orElse(null);
    }
    @Override
    public List<User> getUsersEnAttente() {
        return ur.findByStatutCompte(StatutCompte.EN_ATTENTE);
    }

    @Override
    public List<User> getUsersByProfileValidationStatus(ProfileValidationStatus profileValidationStatus) {
        return ur.findByProfileValidationStatus(profileValidationStatus);
    }

    @Override
    public User updateStatut(Long id, StatutCompte statut) {
        User user = ur.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatutCompte(statut);
        return ur.save(user);
    }

    @Override
    public List<User> getInstitutions() {
        return ur.findByRole(Role.AGENT);
    }


    @Override
    public User reviewProfile(Long id, boolean approved, String motifRefus) {
        User user = ur.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (approved) {
            user.setProfileValidationStatus(ProfileValidationStatus.VALIDATED);
            user.setStatutCompte(StatutCompte.APPROUVE);
            user.setMotifRefus(null);
        } else {
            user.setProfileValidationStatus(ProfileValidationStatus.REJECTED);
            user.setStatutCompte(StatutCompte.REFUSE);
            user.setMotifRefus(motifRefus);
        }

        return ur.save(user);

    }
}

