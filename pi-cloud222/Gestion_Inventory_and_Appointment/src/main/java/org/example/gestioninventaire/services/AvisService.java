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
    private final HybridTextModerationService moderationService;

    @Transactional
    public AvisResponse createAvis(CreateAvisRequest req, Long agriculteurId) {
        if (avisRepo.existsByAgriculteurIdAndVeterinarianId(agriculteurId, req.getVeterinarianId())) {
            throw new BadRequestException("Vous avez déjà évalué ce vétérinaire.");
        }

        validateText(req.getCommentaire());

        Avis avis = Avis.builder()
                .note(req.getNote())
                .commentaire(req.getCommentaire())
                .agriculteurId(agriculteurId)
                .veterinarianId(req.getVeterinarianId())
                .build();

        Avis saved = avisRepo.save(avis);
        return avisMapper.toAvisResponse(saved, agriculteurId);
    }


    @Transactional(readOnly = true)
    public List<AvisResponse> getAvisByVet(Long vetId, Long currentUserId) {
        return avisRepo.findByVeterinarianIdOrderByCreatedAtDesc(vetId)
                .stream()
                .map(a -> avisMapper.toAvisResponse(a, currentUserId))
                .collect(Collectors.toList());
    }


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


    @Transactional
    public CommentaireAvisResponse addCommentaire(Long avisId,
                                                  CommentaireAvisRequest req,
                                                  Long agriculteurId) {
        Avis avis = avisRepo.findById(avisId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable : " + avisId));

        validateText(req.getContenu());

        CommentaireAvis commentaire = CommentaireAvis.builder()
                .contenu(req.getContenu())
                .agriculteurId(agriculteurId)
                .avis(avis)
                .build();

        return avisMapper.toCommentaireResponse(commentaireRepo.save(commentaire));
    }


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

        validateText(req.getContenu());

        ReponseAvis reponse = ReponseAvis.builder()
                .contenu(req.getContenu())
                .veterinarianId(vetId)
                .avis(avis)
                .build();

        return avisMapper.toReponseResponse(reponseRepo.save(reponse));
    }

    private void validateText(String text) {
        TextModerationResult result = moderationService.moderate(text);
        if (!result.allowed()) {
            throw new BadRequestException(result.reason());
        }
    }
}
