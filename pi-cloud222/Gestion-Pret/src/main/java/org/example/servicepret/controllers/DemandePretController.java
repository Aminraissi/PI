package org.example.servicepret.controllers;

import lombok.AllArgsConstructor;
import org.example.servicepret.DTO.DemandePretDTO;
import org.example.servicepret.entities.DemandePret;
import org.example.servicepret.services.IDemandePretService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/demandePret")
public class DemandePretController {

    private IDemandePretService demandePretService;

    @GetMapping("/getAll")
    public List<DemandePret> recupererTousLesDemandePrets()
    {
        return demandePretService.retrieveAllDemandes();
    }
    @PostMapping("/add")
    public DemandePret ajouterDemandePret(@RequestBody DemandePret dp)
    {
        return demandePretService.addDemande(dp);
    }

    @PutMapping("/update")
    public DemandePret updateDemandePret(@RequestBody DemandePret dp)
    {
        return demandePretService.updateDemande(dp);
    }
    @GetMapping("/get/{id}")
    public DemandePret retrieveDemandePret(@PathVariable long id)
    {
        return demandePretService.retrieveDemande(id);
    }

    @DeleteMapping("/delete/{id}")
    public void supprimerDemandePret(@PathVariable long id) {
        demandePretService.removeDemande(id);
    }

    @GetMapping("/{id}/documents")
    public List<String> getDocuments(@PathVariable long id) {
        return demandePretService.getDocumentNames(id);
    }

    @GetMapping("/{id}/documents/{filename}")
    public ResponseEntity<byte[]> getDocument(
            @PathVariable long id,
            @PathVariable String filename,
            @RequestParam("uid") Long userId,
            @RequestParam("data") String data,
            @RequestParam("sig") String sig) {

        byte[] file = demandePretService.getDocument(
                userId,
                id,
                filename,
                data,
                sig
        );

        String contentType = "application/octet-stream";

        if (filename.endsWith(".pdf")) contentType = "application/pdf";
        if (filename.endsWith(".png")) contentType = "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) contentType = "image/jpeg";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filename + "\"")
                .body(file);
    }
    @PostMapping("/{id}/documents")
    public ResponseEntity<?> ajouterDocuments(
            @PathVariable long id,
            @RequestParam("files") List<MultipartFile> files) {
        System.out.println("🔵 [CONTROLLER] ajouterDocuments appelé pour id=" + id + " avec " + files.size() + " fichiers");


        try {
            return ResponseEntity.ok(demandePretService.addDocuments(id, files));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
    @GetMapping("/count/{serviceId}")
    public long countByService(@PathVariable Long serviceId) {
        return demandePretService.countDemandesByService(serviceId);
    }


//    @GetMapping("/service/{id}")
//    public ResponseEntity<List<DemandePret>> getByService(@PathVariable Long id) {
//        return ResponseEntity.ok(demandePretService.getByServiceId(id));
//    }
    @GetMapping("/service/{id}")
    public ResponseEntity<List<DemandePretDTO>> getByService(@PathVariable Long id) {
        return ResponseEntity.ok(
                demandePretService.getByServiceId(id)
                        .stream()
                        .map(demandePretService::toDTO)
                        .toList()
        );
    }
    @GetMapping("/{id}/documents/{filename}/signed-url")
    public ResponseEntity<String> getSignedUrl(
            @PathVariable long id,
            @PathVariable String filename,
            @RequestParam("uid") Long userId,
            @RequestParam(value = "validity", defaultValue = "300000") long validityMillis) {

        String signedUrl = demandePretService.generateSignedUrl(userId, id, filename, validityMillis);
        return ResponseEntity.ok(signedUrl);
    }
    @PostMapping("/{id}/score")
    public ResponseEntity<?> scorer(@PathVariable Long id) {
        DemandePret demande = demandePretService.scorerDemande(id);
        return ResponseEntity.ok(demande);
    }

    @PutMapping("/refuser/{id}")
    public DemandePret refuserDemande(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String motif = body.get("motif");
        return demandePretService.refuserDemande(id, motif);
    }
    @GetMapping("/by-agriculteur/{id}")
    public List<DemandePret> getByAgriculteur(@PathVariable Long id) {
        return demandePretService.getDemandesByAgriculteur(id);
    }
}
