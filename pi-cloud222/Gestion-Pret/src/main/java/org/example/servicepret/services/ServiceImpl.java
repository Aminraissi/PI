package org.example.servicepret.services;

import lombok.AllArgsConstructor;
import org.example.servicepret.entities.ServicePret;
import org.example.servicepret.repositories.ServiceRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ServiceImpl implements IService {

    private ServiceRepo serviceRepo;
    public List<ServicePret> retrieveAllServices()
    {
        return serviceRepo.findAll();
    }
    public ServicePret updateService (ServicePret Service)
    {
        return serviceRepo.save(Service);
    }
    public ServicePret addService (ServicePret Service)
    {
        return serviceRepo.save(Service);
    }
    public ServicePret retrieveService (long idService)
    {
        return serviceRepo.findById(idService).orElse(null);
    }
    public void removeService(long idService)
    {
        serviceRepo.deleteById(idService);
    }
    public List<ServicePret> getServicesByAgent(Long agentId) {
        return serviceRepo.findByAgentId(agentId);
    }


}
