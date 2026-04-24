package org.example.gestionevenement.Config;

import lombok.RequiredArgsConstructor;
import org.example.gestionevenement.Repositories.EventRepo;
import org.example.gestionevenement.entities.Event;
import org.example.gestionevenement.entities.StatutEvent;
import org.example.gestionevenement.entities.TypeEvent;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class EventDataSeeder {

    private final EventRepo eventRepo;

    @Bean
    CommandLineRunner seedEvents() {
        return args -> {
            seedIfMissing(buildWorkshopEvent());
            seedIfMissing(buildMarketEvent());
        };
    }

    private void seedIfMissing(Event event) {
        if (!eventRepo.existsByTitre(event.getTitre())) {
            eventRepo.save(event);
        }
    }

    private Event buildWorkshopEvent() {
        Event event = new Event();
        LocalDateTime start = LocalDateTime.now().plusDays(5).withHour(9).withMinute(0).withSecond(0).withNano(0);

        event.setTitre("Atelier irrigation goutte-a-goutte");
        event.setDescription("Session de demonstration sur l'irrigation goutte-a-goutte, l'optimisation de l'eau et les bonnes pratiques pour les petites exploitations.");
        event.setType(TypeEvent.WORKSHOP);
        event.setDateDebut(start);
        event.setDateFin(start.plusHours(4));
        event.setLieu("INAT");
        event.setMontant(15f);
        event.setImage("event.png");
        event.setRegion("Tunis");
        event.setCapaciteMax(30);
        event.setStatut(StatutEvent.PLANNED);
        event.setInscrits(8);
        event.setAutorisationmunicipale("AUT-TEST-EVT-001");
        event.setLatitude(36.8091);
        event.setLongitude(10.1330);
        event.setGeolocated(true);
        event.setIsValid(true);
        event.setIdOrganisateur(1L);

        return event;
    }

    private Event buildMarketEvent() {
        Event event = new Event();
        LocalDateTime start = LocalDateTime.now().plusDays(12).withHour(10).withMinute(0).withSecond(0).withNano(0);

        event.setTitre("Marche bio des producteurs");
        event.setDescription("Rencontre locale entre producteurs et consommateurs avec vente directe, degustation et presentation de produits agricoles de saison.");
        event.setType(TypeEvent.MARKET);
        event.setDateDebut(start);
        event.setDateFin(start.plusHours(6));
        event.setLieu("Parc Nahli");
        event.setMontant(0f);
        event.setImage("event.png");
        event.setRegion("Ariana");
        event.setCapaciteMax(80);
        event.setStatut(StatutEvent.PLANNED);
        event.setInscrits(22);
        event.setAutorisationmunicipale("AUT-TEST-EVT-002");
        event.setLatitude(36.8625);
        event.setLongitude(10.1604);
        event.setGeolocated(true);
        event.setIsValid(true);
        event.setIdOrganisateur(2L);

        return event;
    }
}
