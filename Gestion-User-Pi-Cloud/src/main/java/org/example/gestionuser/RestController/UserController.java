package org.example.gestionuser.RestController;

import lombok.AllArgsConstructor;
import org.example.gestionuser.Services.IUser;
import org.example.gestionuser.Services.LocalUserFileStorageService;
import org.example.gestionuser.dtos.AdminProfileReviewRequest;
import org.example.gestionuser.dtos.FileUploadResponse;
import org.example.gestionuser.dtos.UserProfileUpdateRequest;
import org.example.gestionuser.entities.ProfileValidationStatus;
import org.example.gestionuser.entities.StatutCompte;
import org.example.gestionuser.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/user")
public class UserController {
    private final IUser iu;
    private final LocalUserFileStorageService localUserFileStorageService;

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllUser() {
        return ResponseEntity.ok(iu.getAllUsers());
    }

    @GetMapping("/getUser/{id}")

    public User getUser(@PathVariable long id) {
        return iu.getUser(id);
    }

    @PostMapping("/addUser")
    public User addUser(@RequestBody User user) {
        return iu.adduser(user);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String url = localUserFileStorageService.store(file);
            return ResponseEntity.ok(new FileUploadResponse(url, file.getOriginalFilename()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not store file");
        }
    }

    @PutMapping("/updateaUser")
    public User update(@RequestBody User user) {
        return iu.updateUser(user);
    }

    @DeleteMapping("/del/{id}")
    public void del(@PathVariable long id) {
        iu.removeUser(id);
    }

    @GetMapping("/hello")
    public String hello() {
        return "hello from user Service";
    }
    @GetMapping("/enAttente")
    public ResponseEntity<?> getUsersEnAttente() {
        return ResponseEntity.ok(iu.getUsersEnAttente());
    }

    @GetMapping("/profile-review/pending")
    public ResponseEntity<?> getUsersPendingProfileReview() {
        return ResponseEntity.ok(iu.getUsersByProfileValidationStatus(ProfileValidationStatus.PENDING_VALIDATION));
    }

    @PutMapping("/updateStatut/{id}")
    public User updateStatut(@PathVariable Long id, @RequestParam StatutCompte statut) {
        return iu.updateStatut(id, statut);
    }

    @GetMapping("/institutions")
    public List<User> getInstitutions() {
        return iu.getInstitutions();
    }

    @PutMapping("/reviewProfile/{id}")
    public User reviewProfile(@PathVariable Long id, @RequestBody AdminProfileReviewRequest request) {
        return iu.reviewProfile(id, request.isApproved(), request.getMotifRefus());
    }
















    //profile
    @PutMapping("/profile/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody UserProfileUpdateRequest request) {
        try {
            return ResponseEntity.ok(iu.updateProfile(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/profile/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfilePhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Image is required");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                return ResponseEntity.badRequest().body("Only image files are allowed");
            }

            User user = iu.getUser(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            String extension = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path uploadDir = Paths.get("uploads", "profiles").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), uploadDir.resolve(filename));

            user.setPhoto("/user/uploads/profiles/" + filename);
            return ResponseEntity.ok(iu.updateUser(user));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Could not save image");
        }
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null) {
            return ".jpg";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return ".jpg";
        }
        return originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }
}
