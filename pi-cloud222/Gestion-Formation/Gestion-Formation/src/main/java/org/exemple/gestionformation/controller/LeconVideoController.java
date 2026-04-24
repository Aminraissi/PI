package org.exemple.gestionformation.controller;


import lombok.RequiredArgsConstructor;
import org.exemple.gestionformation.entity.LeconVideo;
import org.exemple.gestionformation.service.LeconVideoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LeconVideoController {

    private final LeconVideoService leconVideoService;

    @PostMapping("/modules/{moduleId}/lecons")
    @ResponseStatus(HttpStatus.CREATED)
    public LeconVideo create(@PathVariable Long moduleId, @RequestBody LeconVideo leconVideo) {
        return leconVideoService.create(moduleId, leconVideo);
    }

    @PostMapping("/formations/{formationId}/modules/{moduleId}/lecons")
    @ResponseStatus(HttpStatus.CREATED)
    public LeconVideo createForFormationModule(@PathVariable Long formationId, @PathVariable Long moduleId, @RequestBody LeconVideo leconVideo) {
        return leconVideoService.create(moduleId, leconVideo);
    }

    @GetMapping("/lecons")
    public List<LeconVideo> getAll() {
        return leconVideoService.getAll();
    }

    @GetMapping("/lecons/{id}")
    public LeconVideo getById(@PathVariable Long id) {
        return leconVideoService.getById(id);
    }

    @GetMapping("/modules/{moduleId}/lecons")
    public List<LeconVideo> getByModule(@PathVariable Long moduleId) {
        return leconVideoService.getByModule(moduleId);
    }

    @GetMapping("/formations/{formationId}/modules/{moduleId}/lecons")
    public List<LeconVideo> getByFormationModule(@PathVariable Long formationId, @PathVariable Long moduleId) {
        return leconVideoService.getByModule(moduleId);
    }

    @PutMapping("/lecons/{id}")
    public LeconVideo update(@PathVariable Long id, @RequestBody LeconVideo leconVideo) {
        return leconVideoService.update(id, leconVideo);
    }

    @PutMapping("/formations/{formationId}/modules/{moduleId}/lecons/{id}")
    public LeconVideo updateForFormationModule(@PathVariable Long formationId, @PathVariable Long moduleId, @PathVariable Long id, @RequestBody LeconVideo leconVideo) {
        return leconVideoService.update(id, leconVideo);
    }

    @DeleteMapping("/lecons/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        leconVideoService.delete(id);
    }

    @DeleteMapping("/formations/{formationId}/modules/{moduleId}/lecons/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteForFormationModule(@PathVariable Long formationId, @PathVariable Long moduleId, @PathVariable Long id) {
        leconVideoService.delete(id);
    }

    @PostMapping(value = "/lecons/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadVideo(@RequestParam("file") MultipartFile file) {
        String videoUrl = leconVideoService.uploadVideo(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("videoUrl", videoUrl));
    }

    @PostMapping(value = "/formations/{formationId}/modules/{moduleId}/lecons/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadVideoForFormationModule(@PathVariable Long formationId, @PathVariable Long moduleId, @RequestParam("file") MultipartFile file) {
        String videoUrl = leconVideoService.uploadVideo(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("videoUrl", videoUrl));
    }
}
