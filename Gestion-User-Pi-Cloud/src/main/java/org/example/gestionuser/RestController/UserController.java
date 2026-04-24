package org.example.gestionuser.RestController;

import lombok.AllArgsConstructor;
import org.example.gestionuser.Services.IUser;
import org.example.gestionuser.entities.StatutCompte;
import org.example.gestionuser.entities.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/user")
public class UserController {
    private final IUser iu;

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
    @PutMapping("/updateStatut/{id}")
    public User updateStatut(@PathVariable Long id, @RequestParam StatutCompte statut) {
        return iu.updateStatut(id, statut);
    }
}
