package org.example.gestionvente.Controllers;

import org.example.gestionvente.Dtos.ClientSignatureRequest;
import org.example.gestionvente.Dtos.ContractInfoRequest;
import org.example.gestionvente.Dtos.PropositionLocationRequest;
import org.example.gestionvente.Dtos.RefuseProposalRequest;
import org.example.gestionvente.Entities.PropositionLocation;
import org.example.gestionvente.Services.IPropositionLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proposition-location")
public class PropositionLocationController {

    @Autowired
    private IPropositionLocationService service;

    public PropositionLocationController(IPropositionLocationService service) {
        this.service = service;
    }

    @PostMapping("/reservation/{reservationId}")
    public PropositionLocation create(
            @PathVariable("reservationId") Long reservationId,
            @RequestParam("userId") Long userId,
            @RequestBody PropositionLocationRequest request
    ) {
        return service.createFromReservation(reservationId, userId, request);
    }

    @GetMapping("/locataire/{userId}")
    public List<PropositionLocation> getByLocataire(@PathVariable("userId") Long userId) {
        return service.getByLocataire(userId);
    }

    @GetMapping("/agriculteur/{userId}")
    public List<PropositionLocation> getByAgriculteur(@PathVariable("userId") Long userId) {
        return service.getByAgriculteur(userId);
    }

    @PutMapping("/{proposalId}/accept")
    public PropositionLocation accept(
            @PathVariable("proposalId") Long proposalId,
            @RequestParam("agriculteurId") Long agriculteurId
    ) {
        return service.acceptProposal(proposalId, agriculteurId);
    }

    @PutMapping("/{proposalId}/refuse")
    public PropositionLocation refuse(
            @PathVariable("proposalId") Long proposalId,
            @RequestParam("agriculteurId") Long agriculteurId,
            @RequestBody RefuseProposalRequest request
    ) {
        return service.refuseProposal(
                proposalId,
                agriculteurId,
                request != null ? request.getMessageRefus() : null
        );
    }

    @PutMapping("/{proposalId}/contract-info")
    public PropositionLocation saveContractInfo(
            @PathVariable("proposalId") Long proposalId,
            @RequestParam("agriculteurId") Long agriculteurId,
            @RequestBody ContractInfoRequest request
    ) {
        return service.saveContractInfo(proposalId, agriculteurId, request);
    }

    @GetMapping("/{proposalId}")
    public PropositionLocation getById(@PathVariable("proposalId") Long proposalId) {
        return service.getById(proposalId);
    }

    @PutMapping("/{proposalId}/sign-client")
    public PropositionLocation signClientContract(
            @PathVariable("proposalId") Long proposalId,
            @RequestParam("locataireId") Long locataireId,
            @RequestBody ClientSignatureRequest request
    ) {
        return service.signContractByClient(proposalId, locataireId, request);
    }

    @GetMapping("/location/{locationId}")
    public List<PropositionLocation> getByLocation(@PathVariable("locationId") Long locationId) {
        return service.getByLocation(locationId);
    }

}
