package org.exemple.farmersupport.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.exemple.farmersupport.entity.EvenementCalendrier;
import org.exemple.farmersupport.entity.Rappel;
import org.exemple.farmersupport.repository.EvenementCalendrierRepository;
import org.exemple.farmersupport.repository.RappelRepository;
import org.exemple.farmersupport.service.RappelService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RappelServiceImpl implements RappelService {

    private final RappelRepository rappelRepository;
    private final EvenementCalendrierRepository evenementRepository;

    @Override
    public Rappel create(Long eventId, Rappel rappel) {
        EvenementCalendrier event = evenementRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evenement not found with id: " + eventId));
        rappel.setEvenement(event);
        return rappelRepository.save(rappel);
    }

    @Override
    public List<Rappel> getAll() {
        return rappelRepository.findAll();
    }

    @Override
    public List<Rappel> getByEvent(Long eventId) {
        return rappelRepository.findByEvenementIdEvent(eventId);
    }

    @Override
    public Rappel getById(Long id) {
        return rappelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rappel not found with id: " + id));
    }

    @Override
    public Rappel update(Long id, Rappel rappel) {
        Rappel existing = getById(id);
        existing.setDelaiAvantMinutes(rappel.getDelaiAvantMinutes());
        existing.setCanal(rappel.getCanal());
        return rappelRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Rappel existing = getById(id);
        rappelRepository.delete(existing);
    }
}