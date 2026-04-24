package org.example.gestionevenement.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import lombok.AllArgsConstructor;
import org.example.gestionevenement.DTO.DelayEventRequest;
import org.example.gestionevenement.DTO.EventDTO;
import org.example.gestionevenement.DTO.EventNearbyDTO;
import org.example.gestionevenement.Services.IEvent;
import org.example.gestionevenement.Services.IOsrm;
import org.example.gestionevenement.Services.IReservation;
import org.example.gestionevenement.entities.EtatPaiement;
import org.example.gestionevenement.entities.Event;
import org.example.gestionevenement.entities.Reservation;
import org.example.gestionevenement.entities.StatutEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/event")
public class EventController {
    private IEvent ievent;
    private IReservation ireservation;
    private IOsrm osrm;

    @GetMapping("/getAllEvents")
    public List<EventDTO> getAllEvents() {
        return ievent.getAllEvents();
    }

    @GetMapping("/getEvent/{id}")
    public Event getEvent(@PathVariable int id) {
        return ievent.getEvent(id);
    }

    @PutMapping("/validate/{id}")
    public Event validateEvent(@PathVariable int id) {
        return ievent.validateEvent(id);
    }

    @PutMapping("/reject/{id}")
    public Event rejectEvent(@PathVariable int id) {
        return ievent.rejectEvent(id);
    }

    @PostMapping("/addEvent")
    public Event addEvent(@RequestBody Event event) {
        return ievent.addEvent(event);
    }

    @PutMapping("/updateEvent")
    public Event updateEvent(@RequestBody Event event) {
        return ievent.updateEvent(event);
    }

    @DeleteMapping("/deleteEvent/{id}")
    public void deleteEvent(@PathVariable int id) {
        ievent.removeEvent(id);
    }
    @GetMapping("/GetOrganisateurEvents/{id}")
    public List<Event> getEventsByOrganisateur(@PathVariable Long id) {
        return ievent.getEventsByOrganisateur(id);
    }

    @PostMapping("/cancelEvent/{id}")
    public ResponseEntity<Map<String, Object>> cancelEvent(@PathVariable int id) {

        Event event = ievent.getEvent(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        event.setStatut(StatutEvent.CANCELLED);
        ievent.updateEvent(event);

        List<Reservation> reservations = ireservation.getReservationsByEvent(id);

        int refunded = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Reservation r : reservations) {
            if (r.getEtatPaiement() != EtatPaiement.PAID || r.getPaymentIntentId() == null) {
                skipped++;
                continue;
            }
            try {
                Refund.create(
                        RefundCreateParams.builder()
                                .setPaymentIntent(r.getPaymentIntentId())
                                .build()
                );
                r.setEtatPaiement(EtatPaiement.REFUNDED);
                ireservation.updateReservation(r);
                refunded++;
            } catch (StripeException e) {
                errors.add("Reservation " + r.getId() + ": " + e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of(
                "status",   "CANCELLED",
                "refunded", refunded,
                "skipped",  skipped,
                "errors",   errors
        ));
    }
    @PutMapping("/delayEvent/{id}")
    public ResponseEntity<Event> delayEvent(@PathVariable int id, @RequestBody DelayEventRequest req) {

        Event event = ievent.getEvent(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        event.setStatut(StatutEvent.POSTPONED);
        event.setDateDebut(req.getNewDateDebut());
        event.setDateFin(req.getNewDateFin());

        if (req.getAutorisationMunicipale() != null && !req.getAutorisationMunicipale().isBlank()) {
            event.setAutorisationmunicipale(req.getAutorisationMunicipale());
        }

        if (req.getReason() != null && !req.getReason().isBlank()) {
            event.setDescription("[POSTPONED – " + req.getReason() + "] " + event.getDescription());
        }

        Event updated = ievent.updateEvent(event);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/map/all")
    public List<EventNearbyDTO> getAllEventsMap(@RequestParam double lat, @RequestParam double lon) {
        return ievent.findAllForMap(lat, lon);
    }
    @PostMapping("/snap")
    public Object snap(@RequestBody List<double[]> points) {
        return osrm.snapToRoad(points);
    }

    @GetMapping("/route")
    public Object route(@RequestParam double fromLat, @RequestParam double fromLon, @RequestParam double toLat, @RequestParam double toLon) {

        return osrm.getRoute(new double[]{fromLat, fromLon}, new double[]{toLat, toLon}
        );
    }
    @PostMapping("/optimize-route")
    public ResponseEntity<JsonNode> optimizeRoute(@RequestBody Map<String, Object> body) {

        List<Double> userList = (List<Double>) body.get("user");
        double[] user = userList.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();

        List<List<Double>> events = (List<List<Double>>) body.get("events");

        List<double[]> coords = new ArrayList<>();
        for (List<Double> e : events) {
            coords.add(new double[]{e.get(0), e.get(1)});
        }

        JsonNode result = osrm.optimizeTrip(user, coords);

        return ResponseEntity.ok(result);
    }
}

