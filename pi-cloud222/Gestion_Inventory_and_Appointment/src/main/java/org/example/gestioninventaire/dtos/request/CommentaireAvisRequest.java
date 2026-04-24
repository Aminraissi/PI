package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentaireAvisRequest {

    @NotBlank(message = "Le commentaire ne peut pas être vide")
    @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    private String contenu;
}
