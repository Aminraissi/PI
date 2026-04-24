package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.Commande;
import org.example.gestionvente.Dtos.CommandeDetailDto;
import org.example.gestionvente.Dtos.CommandeHistoryDto;

import java.util.List;

public interface ICommandeService {
    Commande checkout(Long userId, Double tip);
    Commande validateCommande(Long commandeId);
    void cancelExpiredCommandes();

    List<CommandeHistoryDto> getPaidOrdersByUser(Long userId);
    CommandeDetailDto getPaidOrderDetails(Long userId, Long commandeId);

    List<Commande> getAllOrdersForAdmin();
    CommandeDetailDto getOrderDetailsForAdmin(Long commandeId);
}