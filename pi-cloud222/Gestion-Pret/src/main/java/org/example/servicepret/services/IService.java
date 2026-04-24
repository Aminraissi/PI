package org.example.servicepret.services;


import org.example.servicepret.entities.ServicePret;

import java.util.List;

public interface IService {
    public List<ServicePret> retrieveAllServices();
    public ServicePret updateService (ServicePret Service);
    public ServicePret addService (ServicePret Service);
    public ServicePret retrieveService (long idService);
    public void removeService(long idService);

    public List<ServicePret> getServicesByAgent(Long agentId);
}
