package org.example.servicepret.services;

import lombok.AllArgsConstructor;
import org.example.servicepret.entities.Pret;
import org.example.servicepret.entities.StatutPret;
import org.example.servicepret.repositories.PretRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
public class PretServiceImpl implements IPretService {
    private PretRepo pretRepo;
    private IEcheanceService echeanceService;

    public List<Pret> retrieveAllPrets()
    {
        return pretRepo.findAll();
    }
    public Pret updatePret (Pret Pret)
    {
        return pretRepo.save(Pret);
    }
    @Override
    public Pret addPret(Pret pret) {
        Pret savedPret = pretRepo.saveAndFlush(pret);
        echeanceService.generateEcheancesForPret(savedPret);
        return savedPret;
    }
    public Pret retrievePret (long idPret)
    {
        return pretRepo.findById(idPret).orElse(null);
    }
    public void removePret(long idPret)
    {
        pretRepo.deleteById(idPret);
    }

    @Override
    public Pret findByDemandeId(Long demandeId) {
        return pretRepo.findByDemande_Id(demandeId).orElse(null);
    }
    @Override
    public List<Pret> getPretByAgriculteur(Long agriculteurId) {
        return pretRepo.findByDemande_AgriculteurId(agriculteurId);
    }


}
