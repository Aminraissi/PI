package org.example.gestionuser.dtos;

import lombok.Data;

@Data
public class AdminProfileReviewRequest {
    private boolean approved;
    private String motifRefus;
}
