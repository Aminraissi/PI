package org.example.gestionevenement.Services;

import org.example.gestionevenement.DTO.DelayEventRequest;
import org.example.gestionevenement.DTO.EventDTO;
import org.example.gestionevenement.DTO.EventNearbyDTO;
import org.example.gestionevenement.entities.Event;
import org.example.gestionevenement.entities.StatutEvent;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface IEvent {

    List<EventDTO> getAllEvents();
    List<EventDTO> getValidatedEvents();
    public Event updateEvent(Event event, MultipartFile image, MultipartFile auth);
    Event addEvent(Event event, MultipartFile image, MultipartFile auth);
    Event getEvent (int idEvent);
    void removeEvent (int idEvent);
    List<Event> getEventsByOrganisateur(Long idOrganisateur);
    Event validateEvent(int idEvent);Event rejectEvent(int idEvent);
    Page<EventDTO> getValidatedEventsFiltered(String type, String region, int page, int size);
    Event delayEvent(int id, DelayEventRequest req, MultipartFile file);
    Map<String, Object> cancelEvent(int id);
    List<EventNearbyDTO> findAllForMap(double userLat, double userLon);
}
