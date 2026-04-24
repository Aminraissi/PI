package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CommentaireAvisRequest;
import org.example.gestioninventaire.dtos.request.CreateAvisRequest;
import org.example.gestioninventaire.dtos.request.ReponseAvisRequest;
import org.example.gestioninventaire.dtos.response.AvisResponse;
import org.example.gestioninventaire.dtos.response.CommentaireAvisResponse;
import org.example.gestioninventaire.dtos.response.ReponseAvisResponse;
import org.example.gestioninventaire.dtos.response.VetRatingSummaryResponse;
import org.example.gestioninventaire.entities.Avis;
import org.example.gestioninventaire.entities.CommentaireAvis;
import org.example.gestioninventaire.entities.LikeAvis;
import org.example.gestioninventaire.entities.ReponseAvis;
import org.example.gestioninventaire.exceptions.BadRequestException;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.mappers.AvisMapper;
import org.example.gestioninventaire.repositories.AvisRepository;
import org.example.gestioninventaire.repositories.CommentaireAvisRepository;
import org.example.gestioninventaire.repositories.LikeAvisRepository;
import org.example.gestioninventaire.repositories.ReponseAvisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvisService {

    private final AvisRepository avisRepo;
    private final LikeAvisRepository likeRepo;
    private final CommentaireAvisRepository commentaireRepo;
    private final ReponseAvisRepository reponseRepo;
    private final AvisMapper avisMapper;

    // ─────────────────────────────────────────────────────────────
    // CRÉER UN AVIS (agriculteur → vétérinaire)
    // ─────────────────────────────────────────────────────────────

    /**
     * Un agriculteur évalue un vétérinaire avec une note (1-5) et un commentaire.
     * Un seul avis est autorisé par couple (agriculteur, vétérinaire).
     */
    @Transactional
    public AvisResponse createAvis(CreateAvisRequest req, Long agriculteurId) {
        if (avisRepo.existsByAgriculteurIdAndVeterinarianId(agriculteurId, req.getVeterinarianId())) {
            throw new BadRequestException("Vous avez déjà évalué ce vétérinaire.");
        }

        Avis avis = Avis.builder()
                .note(req.getNote())
                .commentaire(req.getCommentaire())
                .agriculteurId(agriculteurId)
                .veterinarianId(req.getVeterinarianId())
                .build();

        Avis saved = avisRepo.save(avis);
        return avisMapper.toAvisResponse(saved, agriculteurId);
    }

    // ─────────────────────────────────────────────────────────────
    // LISTE DES AVIS D'UN VÉTÉRINAIRE
    // ─────────────────────────────────────────────────────────────

    /**
     * Retourne tous les avis d'un vétérinaire, du plus récent au plus ancien.
     * Le champ likedByMe est calculé pour l'utilisateur JWT courant.
     */
    @Transactional(readOnly = true)
    public List<AvisResponse> getAvisByVet(Long vetId, Long currentUserId) {
        return avisRepo.findByVeterinarianIdOrderByCreatedAtDesc(vetId)
                .stream()
                .map(a -> avisMapper.toAvisResponse(a, currentUserId))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // RÉSUMÉ / NOTATION GLOBALE D'UN VÉTÉRINAIRE
    // ─────────────────────────────────────────────────────────────

    /**
     * Calcule la moyenne des notes, le total d'avis et la distribution
     * par étoile pour un vétérinaire donné.
     */
    @Transactional(readOnly = true)
    public VetRatingSummaryResponse getRatingSummary(Long vetId) {
        Double moyenne = avisRepo.findAverageNoteByVeterinarianId(vetId);
        long total = avisRepo.countByVeterinarianId(vetId);

        // Initialiser toutes les notes à 0
        Map<Integer, Long> dist = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            dist.put(i, 0L);
        }

        // Remplir avec les données réelles
        List<Object[]> rows = avisRepo.findNoteDistributionByVeterinarianId(vetId);
        for (Object[] row : rows) {
            Integer note  = (Integer) row[0];
            Long    count = (Long)    row[1];
            dist.put(note, count);
        }

        double moyenneArrondie = (moyenne != null)
                ? Math.round(moyenne * 10.0) / 10.0
                : 0.0;

        return VetRatingSummaryResponse.builder()
                .veterinarianId(vetId)
                .moyenneNote(moyenneArrondie)
                .totalAvis(total)
                .distribution(dist)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // TOGGLE LIKE (agriculteur ↔ avis)
    // ─────────────────────────────────────────────────────────────

    /**
     * Si l'agriculteur a déjà liké l'avis → supprime le like.
     * Sinon → ajoute un like.
     * Un agriculteur ne peut pas liker son propre avis.
     */
    @Transactional
    public void toggleLike(Long avisId, Long agriculteurId) {
        Avis avis = avisRepo.findById(avisId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable : " + avisId));

        if (avis.getAgriculteurId().equals(agriculteurId)) {
            throw new BadRequestException("Vous ne pouvez pas liker votre propre avis.");
        }

        Optional<LikeAvis> existingLike =
                likeRepo.findByAvisIdAndAgriculteurId(avisId, agriculteurId);

        if (existingLike.isPresent()) {
            likeRepo.delete(existingLike.get());
        } else {
            likeRepo.save(LikeAvis.builder()
                    .avis(avis)
                    .agriculteurId(agriculteurId)
                    .build());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // COMMENTAIRE D'UN AGRICULTEUR SUR UN AVIS
    // ─────────────────────────────────────────────────────────────

    /**
     * Un agriculteur répond au commentaire d'un autre agriculteur.
     * Plusieurs commentaires sont autorisés par avis.
     */
    @Transactional
    public CommentaireAvisResponse addCommentaire(Long avisId,
                                                  CommentaireAvisRequest req,
                                                  Long agriculteurId) {
        Avis avis = avisRepo.findById(avisId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable : " + avisId));

        CommentaireAvis commentaire = CommentaireAvis.builder()
                .contenu(req.getContenu())
                .agriculteurId(agriculteurId)
                .avis(avis)
                .build();

        return avisMapper.toCommentaireResponse(commentaireRepo.save(commentaire));
    }

    // ─────────────────────────────────────────────────────────────
    // RÉPONSE OFFICIELLE DU VÉTÉRINAIRE
    // ─────────────────────────────────────────────────────────────

    /**
     * Le vétérinaire répond à un avis qui le concerne.
     * Une seule réponse est autorisée par avis.
     * Seul le vétérinaire concerné peut répondre.
     */
    @Transactional
    public ReponseAvisResponse addReponseVet(Long avisId,
                                             ReponseAvisRequest req,
                                             Long vetId) {
        Avis avis = avisRepo.findById(avisId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable : " + avisId));

        if (!avis.getVeterinarianId().equals(vetId)) {
            throw new BadRequestException("Vous n'êtes pas autorisé à répondre à cet avis.");
        }

        if (reponseRepo.existsByAvisId(avisId)) {
            throw new BadRequestException("Vous avez déjà répondu à cet avis.");
        }

        ReponseAvis reponse = ReponseAvis.builder()
                .contenu(req.getContenu())
                .veterinarianId(vetId)
                .avis(avis)
                .build();

        return avisMapper.toReponseResponse(reponseRepo.save(reponse));
    }
}
