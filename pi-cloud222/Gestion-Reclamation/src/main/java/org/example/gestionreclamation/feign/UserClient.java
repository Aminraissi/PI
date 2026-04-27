package org.example.gestionreclamation.feign;

import org.example.gestionreclamation.dto.UserSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "GESTIONUSER", url = "${clients.user-service.url}")
public interface UserClient {

    @GetMapping("/api/user/getUser/{id}")
    UserSummaryDto getUserById(@PathVariable("id") Long id);
}
