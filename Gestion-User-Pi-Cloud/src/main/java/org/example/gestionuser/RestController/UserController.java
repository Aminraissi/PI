package org.example.gestionuser.RestController;

import lombok.AllArgsConstructor;
import org.example.gestionuser.Services.IUser;
import org.example.gestionuser.Services.LocalUserFileStorageService;
import org.example.gestionuser.dtos.AdminProfileReviewRequest;
import org.example.gestionuser.dtos.FileUploadResponse;
import org.example.gestionuser.entities.ProfileValidationStatus;
import org.example.gestionuser.entities.StatutCompte;
import org.example.gestionuser.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.List;

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
}
