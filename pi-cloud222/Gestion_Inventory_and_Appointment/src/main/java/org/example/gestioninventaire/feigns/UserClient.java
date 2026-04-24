package org.example.gestioninventaire.feigns;


import org.example.gestioninventaire.dtos.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "gestion-user")
public interface UserClient {

    @GetMapping("/api/user/getUser/{id}")
    UserResponse getUserById(@PathVariable("id") Long id);
}