package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.*;
import org.example.gestionvente.Repositories.DisponibiliteRepo;
import org.example.gestionvente.Repositories.LocationRepo;
import org.example.gestionvente.Repositories.ReservationVisiteRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DisponibiliteServiceImpl implements IDisponibiliteService {

    @Autowired
    private DisponibiliteRepo disponibiliteRepository;

    @Autowired
    private LocationRepo locationRepository;

    @Autowired
    private ReservationVisiteRepo reservationRepository;

    @Override
    public Disponibilite addDisponibilite(Long locationId, Disponibilite disponibilite) {

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        //  validation simple
        if (disponibilite.getHeureDebut().isAfter(disponibilite.getHeureFin())) {
            throw new RuntimeException("heureDebut doit être avant heureFin");
        }

        //  récupérer les disponibilités existantes du même jour
        List<Disponibilite> existing = disponibiliteRepository
                .findByLocationAndJourSemaine(location, disponibilite.getJourSemaine());

        // vérifier overlap
        for (Disponibilite d : existing) {

            boolean overlap = disponibilite.getHeureDebut().isBefore(d.getHeureFin())
                    && disponibilite.getHeureFin().isAfter(d.getHeureDebut());

            if (overlap) {
                throw new RuntimeException("Chevauchement de disponibilités !");
            }
        }

        // set location
        disponibilite.setLocation(location);

        //save
        return disponibiliteRepository.save(disponibilite);
    }

    @Override
    public Disponibilite updateDisponibilite(Long id, Disponibilite newDispo) {

        Disponibilite existingDispo = disponibiliteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Disponibilité non trouvée"));

        Location location = existingDispo.getLocation();


        boolean hasActiveReservations =
                reservationRepository.existsActiveReservation(location.getId());

        if (hasActiveReservations) {
            throw new RuntimeException(
                    "Impossible de modifier la disponibilité : des réservations actives existent."
            );
        }


        if (newDispo.getHeureDebut().isAfter(newDispo.getHeureFin())) {
            throw new RuntimeException("heureDebut doit être avant heureFin");
        }

        List<Disponibilite> existingList = disponibiliteRepository
                .findByLocationAndJourSemaine(location, newDispo.getJourSemaine());

        for (Disponibilite d : existingList) {

            // ignorer lui-même
            if (!d.getId().equals(id)) {

                boolean overlap = newDispo.getHeureDebut().isBefore(d.getHeureFin())
                        && newDispo.getHeureFin().isAfter(d.getHeureDebut());

                if (overlap) {
                    throw new RuntimeException("Chevauchement de disponibilités !");
                }
            }
        }


        existingDispo.setJourSemaine(newDispo.getJourSemaine());
        existingDispo.setHeureDebut(newDispo.getHeureDebut());
        existingDispo.setHeureFin(newDispo.getHeureFin());

        return disponibiliteRepository.save(existingDispo);
    }

    @Override
    public void deleteDisponibilite(Long id) {

        if (!disponibiliteRepository.existsById(id)) {
            throw new RuntimeException("Disponibilité non trouvée");
        }

        disponibiliteRepository.deleteById(id);
    }

    @Override
    public List<Disponibilite> getAllDisponibilites() {
        return disponibiliteRepository.findAll();
    }

    @Override
    public void updateForLocation(Long locationId, List<Disponibilite> newDispos) {

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        List<Disponibilite> existingDispos =
                disponibiliteRepository.findByLocationId(locationId);

        List<ReservationVisite> reservations =
                reservationRepository.findByLocationId(locationId)
                        .stream()
                        .filter(r -> r.getStatut() == StatutReservation.EN_ATTENTE)
                        .toList();

        // ============================
        // 🔒 booked slots must stay exactly present
        // ============================
        for (ReservationVisite r : reservations) {
            JourSemaine jourReservation = convertToJourSemaine(
                    r.getDateVisite().getDayOfWeek()
            );

            boolean stillExists = newDispos.stream().anyMatch(d ->
                    d.getJourSemaine() == jourReservation &&
                            d.getHeureDebut().equals(r.getHeureDebut()) &&
                            d.getHeureFin().equals(r.getHeureFin())
            );

            if (!stillExists) {
                throw new RuntimeException(
                        "Cannot remove or modify a booked time slot (EN_ATTENTE)."
                );
            }
        }

        // ============================
        // 🔁 delete old non-booked slots not present anymore
        // ============================
        for (Disponibilite old : existingDispos) {

            boolean isBooked = reservations.stream().anyMatch(r ->
                    convertToJourSemaine(r.getDateVisite().getDayOfWeek()) == old.getJourSemaine() &&
                            r.getHeureDebut().equals(old.getHeureDebut()) &&
                            r.getHeureFin().equals(old.getHeureFin())
            );

            boolean stillPresentInNew = newDispos.stream().anyMatch(d ->
                    d.getJourSemaine() == old.getJourSemaine() &&
                            d.getHeureDebut().equals(old.getHeureDebut()) &&
                            d.getHeureFin().equals(old.getHeureFin())
            );

            if (!isBooked && !stillPresentInNew) {
                disponibiliteRepository.delete(old);
            }
        }

        // ============================
        // 💾 save truly new slots
        // ============================
        for (Disponibilite newD : newDispos) {

            boolean alreadyExists = existingDispos.stream().anyMatch(old ->
                    old.getJourSemaine() == newD.getJourSemaine() &&
                            old.getHeureDebut().equals(newD.getHeureDebut()) &&
                            old.getHeureFin().equals(newD.getHeureFin())
            );

            if (!alreadyExists) {
                newD.setId(null); // important
                newD.setLocation(location);
                disponibiliteRepository.save(newD);
            }
        }
    }
    private JourSemaine convertToJourSemaine(java.time.DayOfWeek day) {

        return switch (day) {
            case MONDAY -> JourSemaine.LUNDI;
            case TUESDAY -> JourSemaine.MARDI;
            case WEDNESDAY -> JourSemaine.MERCREDI;
            case THURSDAY -> JourSemaine.JEUDI;
            case FRIDAY -> JourSemaine.VENDREDI;
            case SATURDAY -> JourSemaine.SAMEDI;
            case SUNDAY -> JourSemaine.DIMANCHE;
        };
    }

}
