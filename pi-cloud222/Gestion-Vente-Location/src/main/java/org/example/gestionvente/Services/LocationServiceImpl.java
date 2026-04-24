package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.Disponibilite;
import org.example.gestionvente.Entities.Location;
import org.example.gestionvente.Entities.TypeLocation;
import org.example.gestionvente.Repositories.DisponibiliteRepo;
import org.example.gestionvente.Repositories.LocationRepo;
import org.example.gestionvente.Repositories.ReservationVisiteRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class LocationServiceImpl implements ILocationService {

    @Autowired
    private LocationRepo repo;

    @Autowired
    private ReservationVisiteRepo reservationRepository;

    @Autowired
    private DisponibiliteRepo disponibiliteRepository;

    @Override
    public Location create(Location location) {
        validateLocation(location);
        return repo.save(location);
    }

    private void validateLocation(Location location) {
        if (location.getIdUser() == null) {
            throw new RuntimeException("User is required.");
        }

        if (location.getType() == null) {
            throw new RuntimeException("Rental type is required.");
        }

        if (location.getPrix() == null || location.getPrix() <= 0) {
            throw new RuntimeException("Price must be greater than 0.");
        }

        if (location.getDateDebutLocation() == null) {
            throw new RuntimeException("Start date is required.");
        }

        if (location.getDateFinLocation() == null) {
            throw new RuntimeException("End date is required.");
        }

        if (location.getImage() == null || location.getImage().isBlank()) {
            throw new RuntimeException("Image is required.");
        }

        if (location.getDateFinLocation().isBefore(location.getDateDebutLocation())) {
            throw new RuntimeException("End date must be after start date.");
        }

        if (location.getType() == TypeLocation.materiel) {
            if (location.getMarque() == null || location.getMarque().isBlank()) {
                throw new RuntimeException("Brand is required for machine rental.");
            }

            if (location.getModele() == null || location.getModele().isBlank()) {
                throw new RuntimeException("Model is required for machine rental.");
            }

            if (location.getEtat() == null) {
                throw new RuntimeException("Condition is required for machine rental.");
            }
        }

        if (location.getType() == TypeLocation.terrain) {
            if (location.getLocalisation() == null || location.getLocalisation().isBlank()) {
                throw new RuntimeException("Location is required for land rental.");
            }

            if (location.getSuperficie() == null || location.getSuperficie() <= 0) {
                throw new RuntimeException("Surface area must be greater than 0.");
            }

            if (location.getUniteSuperficie() == null || location.getUniteSuperficie().isBlank()) {
                throw new RuntimeException("Surface unit is required for land rental.");
            }

            if (location.getTypeSol() == null || location.getTypeSol().isBlank()) {
                throw new RuntimeException("Soil type is required for land rental.");
            }
        }
    }
    @Override
    public List<Location> getAll() {
        return repo.findAll();
    }

    @Override
    public Location getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found"));
    }

    @Override
    public List<Location> getByUser(Long userId) {
        return repo.findByIdUser(userId);
    }

    @Override
    public List<Location> getByType(TypeLocation type) {
        return repo.findByType(type);
    }

    @Override
    public List<Location> getDisponibles() {
        return repo.findByDisponibiliteTrue();
    }

    @Override
    public Location update(Long id, Location newLocation) {

        boolean hasActiveReservations =
                reservationRepository.existsActiveReservation(id);

        Location existing = getById(id);

        // ============================
        // 🔒 CRITICAL FIELDS (BLOCK IF RESERVED)
        // ============================
        if (hasActiveReservations) {

            if (newLocation.getPrix() != null ||
                    newLocation.getDateDebutLocation() != null ||
                    newLocation.getDateFinLocation() != null ||
                    newLocation.getLocalisation() != null) {

                throw new RuntimeException(
                        "This rental has active reservations. You can only update non-critical fields."
                );
            }
        }

        // ============================
        // 🔥 SAFE UPDATES
        // ============================

        // always allowed
        if (newLocation.getNom() != null)
            existing.setNom(newLocation.getNom());

        if (newLocation.getDisponibilite() != null)
            existing.setDisponibilite(newLocation.getDisponibilite());

        // 🔒 ONLY IF NO RESERVATIONS
        if (!hasActiveReservations) {

            if (newLocation.getPrix() != null)
                existing.setPrix(newLocation.getPrix());

            if (newLocation.getDateDebutLocation() != null)
                existing.setDateDebutLocation(newLocation.getDateDebutLocation());

            if (newLocation.getDateFinLocation() != null)
                existing.setDateFinLocation(newLocation.getDateFinLocation());

            if (newLocation.getLocalisation() != null)
                existing.setLocalisation(newLocation.getLocalisation());
        }

        // ============================
        // 🌱 TERRAIN (ALWAYS SAFE)
        // ============================

        if (newLocation.getSuperficie() != null)
            existing.setSuperficie(newLocation.getSuperficie());

        if (newLocation.getUniteSuperficie() != null)
            existing.setUniteSuperficie(newLocation.getUniteSuperficie());

        if (newLocation.getTypeSol() != null)
            existing.setTypeSol(newLocation.getTypeSol());

        // ============================
        // 🚜 MATERIEL (ALWAYS SAFE)
        // ============================

        if (newLocation.getMarque() != null)
            existing.setMarque(newLocation.getMarque());

        if (newLocation.getModele() != null)
            existing.setModele(newLocation.getModele());

        if (newLocation.getEtat() != null)
            existing.setEtat(newLocation.getEtat());

        if (newLocation.getImage() != null)
            existing.setImage(newLocation.getImage());

        validateLocation(existing);
        return repo.save(existing);
    }
    @Transactional
    @Override
    public void delete(Long id) {

        boolean hasBlockingReservations =
                reservationRepository.existsBlockingReservation(id);

        if (hasBlockingReservations) {
            throw new RuntimeException(
                    "Cannot delete this rental because it has pending or confirmed reservations."
            );
        }

        // 🔥 DELETE ALL RESERVATIONS (TERMINEE / ANNULEE)
        reservationRepository.deleteByLocationId(id);

        // 🔥 DELETE AVAILABILITIES
        disponibiliteRepository.deleteByLocationId(id);

        // 🔥 DELETE LOCATION
        repo.deleteById(id);
    }

    @Override
    public boolean hasActiveReservations(Long locationId) {
        return reservationRepository.existsActiveReservation(locationId);
    }

    @Override
    public List<Disponibilite> findByLocationId(Long locationId) {
        return disponibiliteRepository.findByLocationId(locationId);
    }
}