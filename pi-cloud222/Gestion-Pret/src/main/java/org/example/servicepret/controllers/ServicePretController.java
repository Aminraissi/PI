package org.example.servicepret.controllers;

import lombok.AllArgsConstructor;
import org.example.servicepret.entities.ServicePret;
import org.example.servicepret.services.IService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/servicePret")
public class ServicePretController {
    private IService service;

    @GetMapping("/getAll")
    public List<ServicePret> recupererTousLesServices()
    {
        return service.retrieveAllServices();
    }
    @PostMapping("/add")
    public ServicePret ajouterService(@RequestBody ServicePret s)
    {
        return service.addService(s);
    }

    @PutMapping("/update/{id}")
    public ServicePret updateService(@PathVariable Long id, @RequestBody ServicePret s) {
        s.setId(id);
        return service.updateService(s);
    }
    @GetMapping("/get/{id}")
    public ServicePret retrieveService(@PathVariable long id)
    {return service.retrieveService(id);}

    @DeleteMapping("/delete/{id}")
    public void supprimerService(@PathVariable long id)
    {
        service.removeService(id);
    }
    @GetMapping("/getAll/{agentId}")
    public List<ServicePret> getServicesByAgent(@PathVariable Long agentId) {
        return service.getServicesByAgent(agentId);
    }
}
