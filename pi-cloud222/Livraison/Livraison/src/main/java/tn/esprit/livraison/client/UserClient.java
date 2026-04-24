package tn.esprit.livraison.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.livraison.client.dto.UserSummaryDto;

@FeignClient(name = "USER", path = "/api/users")
public interface UserClient {

    @GetMapping("/internal/{id}")
    UserSummaryDto getInternalUser(@PathVariable("id") Integer id);
}

