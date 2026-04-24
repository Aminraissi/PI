package org.example.servicepret.services;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class ScoringClientService {

    public Map<String, Object> callScoringAPI(
            List<byte[]> files,
            List<String> fileNames,   // ← NOUVEAU : vrais noms des fichiers
            Long serviceId,
            double montant,
            int duree
    ) {

        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (int i = 0; i < files.size(); i++) {

            byte[] fileBytes = files.get(i);

            // ← CORRIGÉ : utilise le vrai nom (ex: "1732456789_cin.pdf")
            // Python détecte le type depuis le contenu OU depuis le nom
            final String fileName = (fileNames != null && i < fileNames.size())
                    ? fileNames.get(i)
                    : "file" + i + ".pdf";

            ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };

            body.add("files", resource);
        }

        body.add("service_id", serviceId);
        body.add("montant_demande", montant);
        body.add("duree_mois", duree);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        "http://localhost:8000/scorer",
                        request,
                        Map.class
                );

        return response.getBody();
    }
}