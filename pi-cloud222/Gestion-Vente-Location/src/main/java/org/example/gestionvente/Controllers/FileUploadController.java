package org.example.gestionvente.Controllers;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class FileUploadController {

    private final String UPLOAD_DIR = "/home/amine/Desktop/integration pi/pi-cloud222/Gestion-Vente-Location/uploads";

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) throws IOException {

        // create folder if not exists
        Files.createDirectories(Paths.get(UPLOAD_DIR));

        String fileName = file.getOriginalFilename();

        Path path = Paths.get(UPLOAD_DIR + fileName);

        Files.write(path, file.getBytes());

        return fileName;
    }
}