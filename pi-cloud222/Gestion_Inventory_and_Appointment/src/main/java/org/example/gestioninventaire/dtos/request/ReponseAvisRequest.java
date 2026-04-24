package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReponseAvisRequest {

    @NotBlank(message = "La réponse ne peut pas être vide")
    @Size(max = 1000, message = "La réponse ne peut pas dépasser 1000 caractères")
    private String contenu;
}
