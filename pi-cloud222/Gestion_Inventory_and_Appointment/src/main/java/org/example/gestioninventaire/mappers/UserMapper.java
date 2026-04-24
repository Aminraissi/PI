package org.example.gestioninventaire.mappers;

import org.example.gestioninventaire.dtos.response.UserResponse;
import org.example.gestioninventaire.dtos.response.UserSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSummaryResponse toSummary(UserResponse user) {
        if (user == null) return null;

        return UserSummaryResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .role(user.getRole())
                .build();
    }
}
