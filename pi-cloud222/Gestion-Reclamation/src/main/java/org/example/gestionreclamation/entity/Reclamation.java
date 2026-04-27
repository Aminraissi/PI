package org.example.gestionreclamation.entity;

import org.example.gestionreclamation.enums.ReclamationCategory;
import org.example.gestionreclamation.enums.ReclamationPriority;
import org.example.gestionreclamation.enums.ReclamationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reclamation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String subject;

    @Enumerated(EnumType.STRING)
    private ReclamationCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String attachmentUrl;
    private String attachmentFileName;

    @Enumerated(EnumType.STRING)
    private ReclamationStatus status;

    @Enumerated(EnumType.STRING)
    private ReclamationPriority priority;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "reclamation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<ReclamationMessage> messages = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = ReclamationStatus.EN_ATTENTE;
        if (this.priority == null) this.priority = ReclamationPriority.MOYENNE;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
