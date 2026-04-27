package org.example.gestionevenement.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import lombok.AllArgsConstructor;
import org.example.gestionevenement.DTO.DelayEventRequest;
import org.example.gestionevenement.DTO.EventDTO;
import org.example.gestionevenement.DTO.EventNearbyDTO;
import org.example.gestionevenement.Services.FileStorageService;
import org.example.gestionevenement.Services.IEvent;
import org.example.gestionevenement.Services.IOsrm;
import org.example.gestionevenement.entities.Event;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/event")
public class EventController {
    private IEvent ievent;
    private IOsrm osrm;
    private FileStorageService fileStorageService;
    private ObjectMapper objectMapper;


    @GetMapping("/getAllEvents")
    public List<EventDTO> getAllEvents() {
        return ievent.getAllEvents();
    }

    @GetMapping("/validated")
    public List<EventDTO> getValidatedEvents() {
        return ievent.getValidatedEvents();
    }

    @GetMapping("/getEvent/{id}")
    public Event getEvent(@PathVariable int id) {
        return ievent.getEvent(id);
    }

    @GetMapping("/validated-filtered")
    public Page<EventDTO> getValidatedFiltered(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "6") int size, @RequestParam(required = false) String type, @RequestParam(required = false) String region) {
        return ievent.getValidatedEventsFiltered(type, region, page, size);
    }

    @PutMapping("/validate/{id}")
    public Event validateEvent(@PathVariable int id) {
        return ievent.validateEvent(id);
    }

    @PutMapping("/reject/{id}")
    public Event rejectEvent(@PathVariable int id) {
        return ievent.rejectEvent(id);
    }

    @PostMapping(value ="/addEvent",consumes = "multipart/form-data")
    public Event addEvent(@RequestPart("event") Event event, @RequestPart(value = "image", required = false) MultipartFile image, @RequestPart(value = "auth", required = false) MultipartFile auth) {
        return ievent.addEvent(event, image, auth);
    }

    @PutMapping(value = "/updateEvent", consumes = "multipart/form-data")
    public Event updateEvent(@RequestPart("event") Event event, @RequestPart(value = "image", required = false) MultipartFile image, @RequestPart(value = "auth", required = false) MultipartFile auth) {
        return ievent.updateEvent(event, image, auth);
    }

    @GetMapping("/image/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {

        try {
            Path file = fileStorageService.loadFile(filename);
            Resource resource = new UrlResource(file.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
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

        Map<String, Object> result = ievent.cancelEvent(id);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result);
    }

    @PutMapping(value = "/delayEvent/{id}", consumes = "multipart/form-data")
    public ResponseEntity<Event> delayEvent(@PathVariable int id, @RequestPart("data") String dataJson, @RequestPart(value = "file", required = false) MultipartFile file) {
        DelayEventRequest req = null;
        try {
            req = objectMapper.readValue(dataJson, DelayEventRequest.class);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        Event updated = ievent.delayEvent(id, req, file);
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

