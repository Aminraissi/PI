package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gestioninventaire.dtos.payment.StripeCheckoutRequest;
import org.example.gestioninventaire.dtos.payment.StripeCheckoutResponse;
import org.example.gestioninventaire.dtos.request.CommandeItemRequest;
import org.example.gestioninventaire.dtos.request.CommandeRequest;
import org.example.gestioninventaire.dtos.response.CommandeItemResponse;
import org.example.gestioninventaire.dtos.response.CommandeResponse;
import org.example.gestioninventaire.dtos.response.CommandeVetResponse;
import org.example.gestioninventaire.dtos.response.UserResponse;
import org.example.gestioninventaire.entities.Commande;
import org.example.gestioninventaire.entities.CommandeItem;
import org.example.gestioninventaire.entities.InventoryProduct;
import org.example.gestioninventaire.enums.StatutCommande;
import org.example.gestioninventaire.exceptions.BadRequestException;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.feigns.UserClient;
import org.example.gestioninventaire.repositories.CommandeRepository;
import org.example.gestioninventaire.repositories.InventoryProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final InventoryProductRepository productRepository;
    private final UserClient userClient;
    private final StripeService stripeService;   // ← Stripe intégré, plus de PaymentClient

    /**
     * Crée une commande, décrémente le stock et démarre une session Stripe Checkout.
     */
    @Transactional
    public CommandeResponse creerCommande(CommandeRequest request) {

        // Vérification des stocks
        for (CommandeItemRequest item : request.getItems()) {
            InventoryProduct product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable: " + item.getProductId()));

            if (product.getCurrentQuantity() == null || product.getCurrentQuantity() < item.getQuantite()) {
                throw new BadRequestException(
                        "Stock insuffisant pour \"" + product.getNom() + "\". " +
                                "Disponible: " + (product.getCurrentQuantity() != null ? product.getCurrentQuantity() : 0) +
                                " " + product.getUnit()
                );
            }
        }

        double total = request.getItems().stream()
                .mapToDouble(i -> i.getPrixUnitaire() * i.getQuantite())
                .sum();

        Commande commande = Commande.builder()
                .agriculteurId(request.getAgriculteurId())
                .montantTotal(total)
                .dateCommande(LocalDateTime.now())
                .statut(StatutCommande.EN_ATTENTE)
                .build();

        List<CommandeItem> items = request.getItems().stream()
                .map(dto -> {
                    InventoryProduct product = productRepository.findById(dto.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));

                    // Décrémentation du stock
                    product.setCurrentQuantity(product.getCurrentQuantity() - dto.getQuantite());
                    productRepository.save(product);

                    return CommandeItem.builder()
                            .product(product)
                            .vetId(dto.getVetId())
                            .nomProduit(dto.getNomProduit())
                            .vetNom(dto.getVetNom())
                            .vetRegion(dto.getVetRegion())
                            .prixUnitaire(dto.getPrixUnitaire())
                            .quantite(dto.getQuantite())
                            .sousTotal(dto.getPrixUnitaire() * dto.getQuantite())
                            .commande(commande)
                            .build();
                })
                .collect(Collectors.toList());

        commande.setItems(items);
        Commande saved = commandeRepository.save(commande);

        // Appel Stripe direct (plus de Feign vers payment-service)
        StripeCheckoutResponse checkout = stripeService.createCheckoutSession(
                StripeCheckoutRequest.builder()
                        .commandeId(saved.getId())
                        .userId(saved.getAgriculteurId())
                        .montant(total)
                        .productName("Commande GreenRoots #" + saved.getId())
                        .build()
        );

        saved.setStripePaymentIntentId(checkout.getSessionId());
        saved.setStripeClientSecret(checkout.getCheckoutUrl());
        saved = commandeRepository.save(saved);

        log.info("Commande #{} créée - session Stripe {}", saved.getId(), checkout.getSessionId());

        return toResponse(saved);
    }

    /**
     * Confirmation explicite côté frontend après retour Stripe (?payment=success).
     */
    @Transactional
    public void confirmerPaiementCommande(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable: " + commandeId));

        if (commande.getStatut() == StatutCommande.PAYE) {
            log.info("Commande #{} déjà marquée PAYE", commandeId);
            return;
        }

        commande.setStatut(StatutCommande.PAYE);
        commandeRepository.save(commande);
        log.info("Commande #{} marquée PAYE", commandeId);
    }

    /**
     * Confirmation via webhook Stripe (par paymentIntentId).
     */
    @Transactional
    public void confirmerPaiement(String paymentIntentId) {
        commandeRepository.findByStripePaymentIntentId(paymentIntentId)
                .ifPresent(commande -> {
                    commande.setStatut(StatutCommande.PAYE);
                    commandeRepository.save(commande);
                    log.info("Commande #{} marquée PAYE via webhook", commande.getId());
                });
    }

    /**
     * Échec paiement — restaure le stock.
     */
    @Transactional
    public void echouerPaiement(String paymentIntentId) {
        commandeRepository.findByStripePaymentIntentId(paymentIntentId)
                .ifPresent(commande -> {
                    commande.setStatut(StatutCommande.ECHEC);
                    if (commande.getItems() != null) {
                        commande.getItems().forEach(item -> {
                            if (item.getProduct() != null) {
                                item.getProduct().setCurrentQuantity(
                                        item.getProduct().getCurrentQuantity() + item.getQuantite()
                                );
                                productRepository.save(item.getProduct());
                            }
                        });
                    }
                    commandeRepository.save(commande);
                    log.info("Commande #{} marquée ECHEC, stock restauré", commande.getId());
                });
    }

    public List<CommandeResponse> getMesCommandes(Long agriculteurId) {
        return commandeRepository
                .findByAgriculteurIdOrderByDateCommandeDesc(agriculteurId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public CommandeResponse getCommande(Long id) {
        return commandeRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable: " + id));
    }

    public List<CommandeVetResponse> getCommandesByVetId(Long vetId) {
        List<Commande> commandes = commandeRepository.findCommandesByVetId(vetId);

        return commandes.stream().map(c -> {
            UserResponse agriculteur = null;
            try {
                agriculteur = userClient.getUserById(c.getAgriculteurId());
            } catch (Exception e) {
                log.warn("Impossible de récupérer l'agriculteur #{}: {}", c.getAgriculteurId(), e.getMessage());
            }

            List<CommandeItemResponse> itemsVet = c.getItems() == null ? List.of() :
                    c.getItems().stream()
                            .filter(i -> vetId.equals(i.getVetId()))
                            .map(i -> CommandeItemResponse.builder()
                                    .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                                    .vetId(i.getVetId())
                                    .nomProduit(i.getNomProduit())
                                    .vetNom(i.getVetNom())
                                    .vetRegion(i.getVetRegion())
                                    .prixUnitaire(i.getPrixUnitaire())
                                    .quantite(i.getQuantite())
                                    .sousTotal(i.getSousTotal())
                                    .build())
                            .collect(Collectors.toList());

            double totalVet = itemsVet.stream()
                    .mapToDouble(i -> i.getSousTotal() != null ? i.getSousTotal() : 0)
                    .sum();

            return CommandeVetResponse.builder()
                    .id(c.getId())
                    .montantTotal(totalVet)
                    .dateCommande(c.getDateCommande())
                    .statut(c.getStatut())
                    .agriculteurId(c.getAgriculteurId())
                    .agriculteurNom(agriculteur != null ? agriculteur.getNom() : "-")
                    .agriculteurPrenom(agriculteur != null ? agriculteur.getPrenom() : "-")
                    .agriculteurEmail(agriculteur != null ? agriculteur.getEmail() : "-")
                    .agriculteurTelephone(agriculteur != null ? agriculteur.getTelephone() : "-")
                    .agriculteurCin(agriculteur != null ? agriculteur.getCin() : "-")
                    .items(itemsVet)
                    .build();

        }).collect(Collectors.toList());
    }

    private CommandeResponse toResponse(Commande c) {
        List<CommandeItemResponse> items = c.getItems() == null ? List.of() :
                c.getItems().stream().map(i -> CommandeItemResponse.builder()
                        .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                        .vetId(i.getVetId())
                        .nomProduit(i.getNomProduit())
                        .vetNom(i.getVetNom())
                        .vetRegion(i.getVetRegion())
                        .prixUnitaire(i.getPrixUnitaire())
                        .quantite(i.getQuantite())
                        .sousTotal(i.getSousTotal())
                        .build()).collect(Collectors.toList());

        return CommandeResponse.builder()
                .id(c.getId())
                .agriculteurId(c.getAgriculteurId())
                .montantTotal(c.getMontantTotal())
                .dateCommande(c.getDateCommande())
                .statut(c.getStatut())
                .stripeClientSecret(c.getStripeClientSecret())
                .items(items)
                .build();
    }
}