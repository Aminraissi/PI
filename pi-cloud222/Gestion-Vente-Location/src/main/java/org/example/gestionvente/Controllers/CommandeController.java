package org.example.gestionvente.Controllers;

import org.example.gestionvente.Dtos.CommandeDetailDto;
import org.example.gestionvente.Dtos.CommandeHistoryDto;
import org.example.gestionvente.Entities.Commande;
import org.example.gestionvente.Services.ICommandeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commande")
public class CommandeController {

    private final ICommandeService commandeService;

    public CommandeController(ICommandeService commandeService) {
        this.commandeService = commandeService;
    }

    @PostMapping("/checkout")
    public Commande checkout(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "tip", defaultValue = "0") Double tip
    ) {
        return commandeService.checkout(userId, tip);
    }

    @PutMapping("/validate/{commandeId}")
    public Commande validate(@PathVariable("commandeId") Long commandeId) {
        return commandeService.validateCommande(commandeId);
    }

    @GetMapping("/history")
    public List<CommandeHistoryDto> getPaidOrders(@RequestParam("userId") Long userId) {
        return commandeService.getPaidOrdersByUser(userId);
    }

    @GetMapping("/{commandeId}/details")
    public CommandeDetailDto getPaidOrderDetails(
            @PathVariable("commandeId") Long commandeId,
            @RequestParam("userId") Long userId
    ) {
        return commandeService.getPaidOrderDetails(userId, commandeId);
    }

    @GetMapping("/admin/all")
    public List<Commande> getAllOrdersForAdmin() {
        return commandeService.getAllOrdersForAdmin();
    }

    @GetMapping("/admin/{commandeId}/details")
    public CommandeDetailDto getOrderDetailsForAdmin(@PathVariable("commandeId") Long commandeId) {
        return commandeService.getOrderDetailsForAdmin(commandeId);
    }
}