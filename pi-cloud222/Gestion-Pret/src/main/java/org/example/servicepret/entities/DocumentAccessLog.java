package org.example.servicepret.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentAccessLog {

    @Id
    @GeneratedValue
    private Long id;

    private Long userId;
    private Long demandeId;
    private String fileName;
    private String action;

    private LocalDateTime timestamp;

}
