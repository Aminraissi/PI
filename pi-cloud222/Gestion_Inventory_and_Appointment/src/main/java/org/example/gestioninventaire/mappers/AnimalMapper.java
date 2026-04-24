package org.example.gestioninventaire.mappers;


import org.example.gestioninventaire.dtos.response.AnimalSummaryResponse;
import org.example.gestioninventaire.entities.Animal;
import org.springframework.stereotype.Component;

@Component
public class AnimalMapper {

    public AnimalSummaryResponse toSummary(Animal animal) {
        if (animal == null) return null;

        return AnimalSummaryResponse.builder()
                .id(animal.getId())
                .espece(animal.getEspece())
                .poids(animal.getPoids())
                .reference(animal.getReference())
                .dateNaissance(animal.getDateNaissance())
                .build();
    }
}
