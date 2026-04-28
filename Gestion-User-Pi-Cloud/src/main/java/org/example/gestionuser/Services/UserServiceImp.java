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
    public User findByTelephone(String telephone) {
        return ur.findByTelephone(telephone).orElse(null);
    }

    @Override
    public User updatePasswordByPhone(String telephone, String newPassword) {
        User user = ur.findByTelephone(telephone)
                .orElseThrow(() -> new RuntimeException("No account found with this phone number"));

        user.setMotDePasse(newPassword);
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







//profile
    @Override

    public User updateProfile(Long id, UserProfileUpdateRequest request) {
        User user = ur.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin profile cannot be edited from this page");
        }

        if (request.getNom() != null) user.setNom(request.getNom());
        if (request.getPrenom() != null) user.setPrenom(request.getPrenom());
        if (request.getPhoto() != null) user.setPhoto(request.getPhoto());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getMotDePasse() != null && !request.getMotDePasse().isBlank()) {
            user.setMotDePasse(request.getMotDePasse());
        }
        if (request.getTelephone() != null) user.setTelephone(request.getTelephone());
        if (request.getRegion() != null) user.setRegion(request.getRegion());
        if (request.getCin() != null) user.setCin(request.getCin());

        if (user.getRole() == Role.VETERINAIRE) {
            if (request.getAdresseCabinet() != null) user.setAdresseCabinet(request.getAdresseCabinet());
            if (request.getPresentationCarriere() != null) user.setPresentationCarriere(request.getPresentationCarriere());
            if (request.getTelephoneCabinet() != null) user.setTelephoneCabinet(request.getTelephoneCabinet());
        }

        if (user.getRole() == Role.AGENT) {
            if (request.getAgence() != null) user.setAgence(request.getAgence());
            if (request.getCertificatTravail() != null) user.setCertificatTravail(request.getCertificatTravail());
        }

        if (user.getRole() == Role.ORGANISATEUR_EVENEMENT) {
            if (request.getNomOrganisation() != null) user.setNom_organisation(request.getNomOrganisation());
            if (request.getLogoOrganisation() != null) user.setLogo_organisation(request.getLogoOrganisation());
            if (request.getDescription() != null) user.setDescription(request.getDescription());
        }

        return ur.save(user);
    }
}

