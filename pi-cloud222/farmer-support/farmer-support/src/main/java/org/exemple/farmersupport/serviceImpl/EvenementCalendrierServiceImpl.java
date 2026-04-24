package org.exemple.farmersupport.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.exemple.farmersupport.entity.EvenementCalendrier;
import org.exemple.farmersupport.repository.EvenementCalendrierRepository;
import org.exemple.farmersupport.service.EvenementCalendrierService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvenementCalendrierServiceImpl implements EvenementCalendrierService {

    private final EvenementCalendrierRepository evenementRepository;

    @Override
    public EvenementCalendrier create(EvenementCalendrier evenement) {
        return evenementRepository.save(evenement);
    }

    @Override
    public List<EvenementCalendrier> getAll() {
        return evenementRepository.findAll();
    }

    @Override
    public EvenementCalendrier getById(Long id) {
        return evenementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evenement not found with id: " + id));
    }

    @Override
    public EvenementCalendrier update(Long id, EvenementCalendrier evenement) {
        EvenementCalendrier existing = getById(id);
        existing.setTitre(evenement.getTitre());
        existing.setDescription(evenement.getDescription());
        existing.setDateDebut(evenement.getDateDebut());
        existing.setDateFin(evenement.getDateFin());
        existing.setType(evenement.getType());
        existing.setPriorite(evenement.getPriorite());
        existing.setStatut(evenement.getStatut());
        existing.setUserId(evenement.getUserId());
        return evenementRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        EvenementCalendrier existing = getById(id);
        evenementRepository.delete(existing);
    }
}