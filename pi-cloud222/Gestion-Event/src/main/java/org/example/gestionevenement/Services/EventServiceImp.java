package org.example.gestionevenement.Services;

import lombok.AllArgsConstructor;
import org.example.gestionevenement.DTO.EventDTO;
import org.example.gestionevenement.DTO.EventNearbyDTO;
import org.example.gestionevenement.Repositories.EventRepo;
import org.example.gestionevenement.entities.Event;
import org.example.gestionevenement.entities.StatutEvent;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class EventServiceImp implements IEvent {

    private final EventRepo eventRepo;
    private final GeocodingService geocodingService;
    private final OsrmService osrmService;

    @Override
    public List<EventDTO> getAllEvents() {
        return eventRepo.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public Event validateEvent(int idEvent) {
        Event event = eventRepo.findById(idEvent).orElse(null);
        event.setIsValid(true);
        return eventRepo.save(event);
    }

    @Override
    public Event rejectEvent(int idEvent) {
        Event event = eventRepo.findById(idEvent).orElse(null);
        event.setIsValid(false);
        event.setStatut(StatutEvent.CANCELLED);
        return eventRepo.save(event);
    }

    @Override
    public Event addEvent(Event event) {
        geocodeIfNeeded(event);
        return eventRepo.save(event);
    }

    @Override
    public Event updateEvent(Event event) {
        geocodeIfNeeded(event);
        return eventRepo.save(event);
    }

    @Override
    public Event getEvent(int id) {
        return eventRepo.findById(id).orElse(null);
    }

    @Override
    public void removeEvent(int id) {
        eventRepo.deleteById(id);
    }

    @Override
    public List<Event> getEventsByOrganisateur(Long id) {
        return eventRepo.findByIdOrganisateur(id);
    }

    private EventDTO mapToDTO(Event event) {
        EventDTO dto = new EventDTO();
        dto.setId(event.getId());
        dto.setTitre(event.getTitre());
        dto.setType(event.getType().name());
        dto.setDateDebut(event.getDateDebut());
        dto.setDateFin(event.getDateFin());
        dto.setLieu(event.getLieu());
        dto.setMontant(event.getMontant());
        dto.setImage(event.getImage());
        dto.setRegion(event.getRegion());
        dto.setCapaciteMax(event.getCapaciteMax());
        dto.setInscrits(event.getInscrits());
        dto.setAutorisationmunicipale(event.getAutorisationmunicipale());
        return dto;
    }


    private void geocodeIfNeeded(Event event) {
        if (event.getLatitude() != null && event.getLongitude() != null) return;

        double[] coords = geocodingService.geocode(
                event.getLieu() + "," + event.getRegion()
        );

        if (coords != null) {
            event.setLatitude(coords[0]);
            event.setLongitude(coords[1]);
            event.setGeolocated(true);
        }
    }

    @Override
    public List<EventNearbyDTO> findAllForMap(double userLat, double userLon) {

        List<EventNearbyDTO> events = new java.util.ArrayList<>(
                eventRepo.findAllEventsMap()
                        .stream()
                        .map(this::mapToNearbyDTO)
                        .filter(e -> e.getLatitude() != null && e.getLongitude() != null)
                        .toList()
        );

        if (events.isEmpty()) return events;

        List<double[]> coords = events.stream()
                .map(e -> new double[]{e.getLatitude(), e.getLongitude()})
                .toList();

        Map<String, Object> matrix;
        try {
            matrix = osrmService.getMatrix(coords, new double[]{userLat, userLon});
        } catch (Exception e) {
            e.printStackTrace();
            return events;
        }

        List<List<Object>> durations =
                (List<List<Object>>) matrix.get("durations");

        List<List<Object>> distances =
                (List<List<Object>>) matrix.get("distances");

        if (durations == null || distances == null || durations.isEmpty()) {
            return events;
        }

        for (int i = 0; i < events.size(); i++) {

            List<Object> rowDur = durations.get(0);
            List<Object> rowDist = distances.get(0);

            if (rowDur.size() > i + 1) {
                Object d = rowDur.get(i + 1);
                if (d instanceof Number) {
                    int seconds = ((Number) d).intValue();
                    events.get(i).setDurationSeconds(seconds);
                    events.get(i).setCarMinutes(seconds / 60);
                }
            }

            if (rowDist.size() > i + 1) {
                Object d = rowDist.get(i + 1);
                if (d instanceof Number) {
                    int meters = ((Number) d).intValue();
                    events.get(i).setDistanceMeters(meters);

                    events.get(i).setDistanceKm(meters / 1000.0);
                }
            }

            if (events.get(i).getDistanceKm() != null) {
                double km = events.get(i).getDistanceKm();

                events.get(i).setWalkMinutes((int) (km / 5 * 60));
                events.get(i).setBikeMinutes((int) (km / 15 * 60));
            }
        }

        events.sort((a, b) -> {
            if (a.getDurationSeconds() == null) return 1;
            if (b.getDurationSeconds() == null) return -1;
            return a.getDurationSeconds() - b.getDurationSeconds();
        });

        return events;
    }


    private EventNearbyDTO mapToNearbyDTO(Object[] r) {

        int cap = r[1] != null ? ((Number) r[1]).intValue() : 1;
        int ins = r[6] != null ? ((Number) r[6]).intValue() : 0;

        EventNearbyDTO dto = new EventNearbyDTO();

        dto.setId(((Number) r[0]).intValue());
        dto.setCapaciteMax(cap);
        dto.setDateDebut(((Timestamp) r[2]).toLocalDateTime());
        dto.setDateFin(((Timestamp) r[3]).toLocalDateTime());
        dto.setDescription((String) r[4]);
        dto.setImage((String) r[5]);
        dto.setInscrits(ins);

        dto.setLatitude(((Number) r[7]).doubleValue());
        dto.setLongitude(((Number) r[9]).doubleValue());

        dto.setLieu((String) r[8]);
        dto.setRegion((String) r[11]);
        dto.setMontant(r[10] != null ? ((Number) r[10]).floatValue() : 0f);
        dto.setStatut((String) r[12]);
        dto.setTitre((String) r[13]);
        dto.setType((String) r[14]);

        dto.setFillPercent(cap > 0 ? (ins * 100 / cap) : 0
        );

        return dto;
    }
}
