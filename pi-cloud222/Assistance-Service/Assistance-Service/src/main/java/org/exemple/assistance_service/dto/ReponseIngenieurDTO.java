package org.exemple.assistance_service.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReponseIngenieurDTO {
    private Long idReponse;
    private String contenu;
    private LocalDateTime dateReponse;
    private String statut;
    private Long affectationId;
}