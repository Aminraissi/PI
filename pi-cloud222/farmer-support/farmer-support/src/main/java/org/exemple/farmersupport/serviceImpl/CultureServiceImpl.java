package org.exemple.farmersupport.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.exemple.farmersupport.entity.Culture;
import org.exemple.farmersupport.entity.Parcelle;
import org.exemple.farmersupport.repository.CultureRepository;
import org.exemple.farmersupport.repository.ParcelleRepository;
import org.exemple.farmersupport.service.CultureService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CultureServiceImpl implements CultureService {

    private final CultureRepository cultureRepository;
    private final ParcelleRepository parcelleRepository;

    @Override
    public Culture create(Long parcelleId, Culture culture) {
        Parcelle parcelle = parcelleRepository.findById(parcelleId)
                .orElseThrow(() -> new RuntimeException("Parcelle not found with id: " + parcelleId));
        culture.setParcelle(parcelle);
        return cultureRepository.save(culture);
    }

    @Override
    public List<Culture> getAll() {
        return cultureRepository.findAll();
    }

    @Override
    public List<Culture> getByParcelle(Long parcelleId) {
        return cultureRepository.findByParcelleIdParcelle(parcelleId);
    }

    @Override
    public Culture getById(Long id) {
        return cultureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Culture not found with id: " + id));
    }

    @Override
    public Culture update(Long id, Culture culture) {
        Culture existing = getById(id);
        existing.setEspece(culture.getEspece());
        existing.setVariete(culture.getVariete());
        existing.setDateSemis(culture.getDateSemis());
        existing.setDateRecoltePrevue(culture.getDateRecoltePrevue());
        existing.setStade(culture.getStade());
        existing.setObjectif(culture.getObjectif());
        return cultureRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Culture existing = getById(id);
        cultureRepository.delete(existing);
    }
}