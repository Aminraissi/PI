package org.example.gestionuser.Repositories;


import org.example.gestionuser.entities.Role;

import org.example.gestionuser.entities.ProfileValidationStatus;

import org.example.gestionuser.entities.StatutCompte;
import org.example.gestionuser.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    List<User> findByStatutCompte(StatutCompte statutCompte);

    Optional<User> findByTelephone(String telephone);


    List<User> findByRole(Role role);

    List<User> findByProfileValidationStatus(ProfileValidationStatus profileValidationStatus);


}
