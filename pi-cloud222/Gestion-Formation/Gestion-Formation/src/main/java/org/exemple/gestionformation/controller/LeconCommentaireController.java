package org.exemple.gestionformation.controller;

import lombok.RequiredArgsConstructor;
import org.exemple.gestionformation.entity.LeconCommentaire;
import org.exemple.gestionformation.entity.LeconVideo;
import org.exemple.gestionformation.repository.LeconCommentaireRepository;
import org.exemple.gestionformation.repository.LeconVideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LeconCommentaireController {

    private final LeconCommentaireRepository commentaireRepository;
    private final LeconVideoRepository leconVideoRepository;

    @GetMapping("/lecons/{leconId}/commentaires")
    public List<LeconCommentaire> getByLecon(@PathVariable Long leconId) {
        return commentaireRepository.findByLeconIdLeconOrderByDateCreationAsc(leconId);
    }

    @GetMapping("/formations/{formationId}/modules/{moduleId}/lecons/{leconId}/commentaires")
    public List<LeconCommentaire> getByFormationModuleLecon(@PathVariable Long formationId, @PathVariable Long moduleId, @PathVariable Long leconId) {
        return commentaireRepository.findByLeconIdLeconOrderByDateCreationAsc(leconId);
    }

    @PostMapping("/lecons/{leconId}/commentaires")
    @ResponseStatus(HttpStatus.CREATED)
    public LeconCommentaire create(@PathVariable Long leconId, @RequestBody LeconCommentaire commentaire) {
        return createForLecon(leconId, commentaire);
    }

    @PostMapping("/formations/{formationId}/modules/{moduleId}/lecons/{leconId}/commentaires")
    @ResponseStatus(HttpStatus.CREATED)
    public LeconCommentaire createForFormationModuleLecon(@PathVariable Long formationId, @PathVariable Long moduleId, @PathVariable Long leconId, @RequestBody LeconCommentaire commentaire) {
        return createForLecon(leconId, commentaire);
    }

    @DeleteMapping("/commentaires/{commentaireId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long commentaireId) {
        commentaireRepository.deleteById(commentaireId);
    }

    private LeconCommentaire createForLecon(Long leconId, LeconCommentaire commentaire) {
        LeconVideo lecon = leconVideoRepository.findById(leconId)
                .orElseThrow(() -> new RuntimeException("Lecon not found with id: " + leconId));

        commentaire.setLecon(lecon);
        commentaire.setDateCreation(LocalDateTime.now());
        return commentaireRepository.save(commentaire);
    }
}
