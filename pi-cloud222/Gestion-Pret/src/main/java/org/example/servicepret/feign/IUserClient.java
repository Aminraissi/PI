package org.example.servicepret.feign;

import org.example.servicepret.DTO.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "gestion-user")
public interface IUserClient {

    @GetMapping("/api/user/getUser/{id}")
    User getUser(@PathVariable("id") Long id);
}
