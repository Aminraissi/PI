package org.example.gestionevenement.Services;

import org.example.gestionevenement.DTO.EventDTO;
import org.example.gestionevenement.DTO.EventNearbyDTO;
import org.example.gestionevenement.entities.Event;

import java.util.List;

public interface IEvent {
//    List<Event> getAllEvents();
    List<EventDTO> getAllEvents();
    Event updateEvent (Event event);
    Event addEvent (Event event);
    Event getEvent (int idEvent);
    void removeEvent (int idEvent);
    List<Event> getEventsByOrganisateur(Long idOrganisateur);
    public Event validateEvent(int idEvent);
    public Event rejectEvent(int idEvent);
    List<EventNearbyDTO> findAllForMap(double userLat, double userLon);
}
