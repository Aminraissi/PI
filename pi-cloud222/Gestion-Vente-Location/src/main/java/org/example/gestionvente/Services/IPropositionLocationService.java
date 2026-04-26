package org.example.gestionvente.Services;

import org.example.gestionvente.Dtos.ClientSignatureRequest;
import org.example.gestionvente.Dtos.ContractInfoRequest;
import org.example.gestionvente.Dtos.PropositionLocationRequest;
import org.example.gestionvente.Entities.PropositionLocation;

import java.util.List;

public interface IPropositionLocationService {

    PropositionLocation createFromReservation(Long reservationId, Long userId, PropositionLocationRequest request);

    List<PropositionLocation> getByLocataire(Long userId);

    List<PropositionLocation> getByAgriculteur(Long userId);

    PropositionLocation acceptProposal(Long proposalId, Long agriculteurId);

    PropositionLocation refuseProposal(Long proposalId, Long agriculteurId, String messageRefus);

    PropositionLocation saveContractInfo(Long proposalId, Long agriculteurId, ContractInfoRequest request);

    PropositionLocation getById(Long proposalId);

    PropositionLocation signContractByClient(Long proposalId, Long locataireId, ClientSignatureRequest request);

    List<PropositionLocation> getByLocation(Long locationId);

    boolean hasFinalizedPropositionForUserAndLocation(Long userId, Long locationId);

    List<PropositionLocation> getAll();
}