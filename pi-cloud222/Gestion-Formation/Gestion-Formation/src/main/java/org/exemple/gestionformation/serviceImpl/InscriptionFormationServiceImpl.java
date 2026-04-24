package org.exemple.gestionformation.serviceImpl;



import lombok.RequiredArgsConstructor;
import org.exemple.gestionformation.entity.Formation;
import org.exemple.gestionformation.entity.InscriptionFormation;
import org.exemple.gestionformation.repository.FormationRepository;
import org.exemple.gestionformation.repository.InscriptionFormationRepository;
import org.exemple.gestionformation.service.InscriptionFormationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InscriptionFormationServiceImpl implements InscriptionFormationService {

    private final InscriptionFormationRepository inscriptionFormationRepository;
    private final FormationRepository formationRepository;

    public InscriptionFormation create(Long formationId, InscriptionFormation inscription) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new RuntimeException("Formation not found with id: " + formationId));

        inscription.setFormation(formation);

        if (inscription.getDateInscription() == null) {
            inscription.setDateInscription(LocalDate.now());
        }

        if (inscription.getProgression() == null) {
            inscription.setProgression(0.0);
        }

        return inscriptionFormationRepository.save(inscription);
    }

    public List<InscriptionFormation> getAll() {
        return inscriptionFormationRepository.findAll();
    }

    public InscriptionFormation getById(Long id) {
        return inscriptionFormationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscription not found with id: " + id));
    }

    public List<InscriptionFormation> getByFormation(Long formationId) {
        return inscriptionFormationRepository.findByFormationIdFormation(formationId);
    }

    public List<InscriptionFormation> getByUser(Long userId) {
        return inscriptionFormationRepository.findByUserId(userId);
    }

    public InscriptionFormation update(Long id, InscriptionFormation newData) {
        InscriptionFormation inscription = getById(id);
        inscription.setDateInscription(newData.getDateInscription());
        inscription.setStatutAcces(newData.getStatutAcces());
        inscription.setProgression(newData.getProgression());
        inscription.setUserId(newData.getUserId());
        return inscriptionFormationRepository.save(inscription);
    }

    public void delete(Long id) {
        InscriptionFormation inscription = getById(id);
        inscriptionFormationRepository.delete(inscription);
    }
}
