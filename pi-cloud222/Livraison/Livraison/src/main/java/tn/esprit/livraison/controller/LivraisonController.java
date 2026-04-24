package tn.esprit.livraison.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.livraison.controller.dto.BestDayChatbotRequest;
import tn.esprit.livraison.controller.dto.CreateTargetedDeliveryRequest;
import tn.esprit.livraison.controller.dto.DeliveryPriceEstimateRequest;
import tn.esprit.livraison.controller.dto.DeliveryPriceEstimateResponse;
import tn.esprit.livraison.controller.dto.FarmerChatbotRequest;
import tn.esprit.livraison.controller.dto.FarmerChatbotResponse;
import tn.esprit.livraison.controller.dto.FarmerKnownTransporterDto;
import tn.esprit.livraison.controller.dto.RoutingBestPathRequest;
import tn.esprit.livraison.controller.dto.RoutingBestPathResponse;
import tn.esprit.livraison.controller.dto.WeatherSnapshotDto;
import tn.esprit.livraison.entity.Livraison;
import tn.esprit.livraison.enums.StatusLivraison;
import tn.esprit.livraison.service.GroqChatbotProxyService;
import tn.esprit.livraison.service.LivraisonService;
import tn.esprit.livraison.service.RoutingBestPathService;

@RestController
@RequestMapping("/api/livraisons")
@Tag(name = "Livraisons", description = "API de gestion des livraisons")
public class LivraisonController {

    private final LivraisonService livraisonService;
    private final RoutingBestPathService routingBestPathService;
    private final GroqChatbotProxyService groqChatbotProxyService;

    public LivraisonController(
            LivraisonService livraisonService,
            RoutingBestPathService routingBestPathService,
            GroqChatbotProxyService groqChatbotProxyService) {
        this.livraisonService = livraisonService;
        this.routingBestPathService = routingBestPathService;
        this.groqChatbotProxyService = groqChatbotProxyService;
    }

    @PostMapping("/routing/best-path")
    @Operation(description = "Calculer un chemin depuis le backend")
    public RoutingBestPathResponse calculateBestPath(@RequestBody RoutingBestPathRequest request) {
        return routingBestPathService.computeBestPath(request);
    }

    @GetMapping("/meteo")
    @Operation(description = "Recuperer la meteo courante pour un point")
    public WeatherSnapshotDto getWeather(@RequestParam double lat, @RequestParam double lng) {
        return livraisonService.getWeatherSnapshot(lat, lng);
    }

    @PostMapping("/pricing/estimate")
    @Operation(description = "Estimer le prix cote backend selon meteo, distance et poids")
    public DeliveryPriceEstimateResponse estimatePrice(@RequestBody DeliveryPriceEstimateRequest request) {
        return livraisonService.estimatePrice(request);
    }

    @PostMapping("/chatbot/farmer/ask")
    @Operation(description = "Assistant chatbot pour agriculteur")
    public FarmerChatbotResponse askFarmerChatbot(@RequestBody FarmerChatbotRequest request) {
        return groqChatbotProxyService.askFarmerAssistant(request != null ? request.message() : "");
    }

    @PostMapping("/chatbot/farmer/best-day")
    @Operation(description = "Assistant chatbot - recommandation du meilleur jour pour planifier une livraison")
    public FarmerChatbotResponse askFarmerBestDeliveryDay(@RequestBody BestDayChatbotRequest request) {
        return groqChatbotProxyService.askBestDeliveryDay(request);
    }

    @GetMapping("/agriculteur/{agriculteurId}/transporteurs-preferes")
    @Operation(description = "Liste des transporteurs ayant deja livre cet agriculteur")
    public List<FarmerKnownTransporterDto> getFarmerKnownTransporters(@PathVariable int agriculteurId) {
        return livraisonService.getFarmerKnownTransporters(agriculteurId);
    }

    @PostMapping("/agriculteur/{agriculteurId}/livraisons/cibler-transporteur")
    @Operation(description = "Creer une demande de livraison ciblee vers un transporteur")
    public Livraison createTargetedDelivery(
            @PathVariable int agriculteurId,
            @RequestBody CreateTargetedDeliveryRequest request) {
        return livraisonService.createFarmerTargetedRequest(agriculteurId, request);
    }

    @PostMapping
    @Operation(description = "Ajouter une livraison")
    public Livraison addlivraison(@RequestBody Livraison livraison) {
        return livraisonService.addLivraison(livraison);
    }

    @GetMapping
    @Operation(description = "Récupérer toutes les livraisons (filtrables par agriculteur ou transporteur)")
    public List<Livraison> findAll(
            @RequestParam(required = false) Integer agriculteurId,
            @RequestParam(required = false) Integer transporteurId) {
        if (agriculteurId  != null) return livraisonService.getLivraisonsByAgriculteur(agriculteurId);
        if (transporteurId != null) return livraisonService.getLivraisonsByTransporteur(transporteurId);
        return livraisonService.getAllLivraison();
    }

    @GetMapping("/admin/kpis")
    @Operation(description = "KPIs globaux admin")
    public Map<String, Object> adminKpis() {
        return livraisonService.buildAdminKpis();
    }

    @GetMapping("/{id}")
    @Operation(description = "Récupérer une livraison par ID")
    public Livraison findById(@PathVariable Integer id) {
        return livraisonService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(description = "Modifier une livraison")
    public Livraison update(@PathVariable Integer id, @RequestBody Livraison payload) {
        return livraisonService.updateLivraison(id, payload);
    }

    @DeleteMapping("/{id}")
    @Operation(description = "Supprimer une livraison")
    public void delete(@PathVariable Integer id) {
        livraisonService.delete(id);
    }

    @PutMapping("/{id}/status")
    @Operation(description = "Mettre à jour le statut d'une livraison")
    public Livraison updateStatus(@PathVariable Integer id, @RequestParam StatusLivraison status) {
        return livraisonService.updateStatus(id, status);
    }

    @PutMapping("/{id}/assign")
    @Operation(description = "Affecter manuellement un livreur")
    public Livraison assignTransporteur(@PathVariable Integer id, @RequestParam int transporteurId) {
        return livraisonService.assignTransporteur(id, transporteurId);
    }

    @PostMapping("/{id}/start-route")
    @Operation(description = "Marquer une livraison en route vers l'agriculteur et notifier")
    public Livraison startRouteToPickup(@PathVariable Integer id, @RequestParam int transporteurId) {
        return livraisonService.startRouteToPickup(id, transporteurId);
    }

    @PutMapping("/{id}/gps")
    @Operation(description = "Mettre à jour la position GPS du transporteur")
    public Livraison updateCurrentPosition(
            @PathVariable Integer id,
            @RequestParam double lat,
            @RequestParam double lng) {
        return livraisonService.updateCurrentPosition(id, lat, lng);
    }

    @PutMapping("/{id}/note")
    @Operation(description = "Évaluer le transporteur sur 5")
    public Livraison evaluerTransporteur(
            @PathVariable Integer id,
            @RequestParam double note,
            @RequestParam(required = false) Integer evaluateurId) {
        return livraisonService.evaluerTransporteur(id, note, evaluateurId);
    }

    @PostMapping("/{id}/signature")
    @Operation(description = "Enregistrer la signature numérique de l'agriculteur pour confirmer la réception")
    public Livraison saveSignature(
            @PathVariable Integer id,
            @RequestParam int agriculteurId,
            @RequestBody Map<String, String> body) {
        String signatureData = body.getOrDefault("signatureData", "");
        return livraisonService.saveSignature(id, signatureData, agriculteurId);
    }

    @PostMapping("/{id}/rating/ignore")
    @Operation(description = "Ignorer l'interface de notation pour une livraison livrée")
    public Livraison ignorerNotationTransporteur(
            @PathVariable Integer id,
            @RequestParam int agriculteurId) {
        return livraisonService.ignorerNotationTransporteur(id, agriculteurId);
    }

    // ── Transporter Dashboard & Tools ──────────────────────────────────────────

    @GetMapping("/transporter/{id}/stats")
    @Operation(description = "Statistiques d'activite pour un transporteur")
    public Map<String, Object> getTransporterStats(@PathVariable int id) {
        return livraisonService.buildTransporterStats(id);
    }

    @GetMapping("/transporter/{id}/advanced-stats")
    @Operation(description = "Statistiques avancées sur une période donnée")
    public Map<String, Object> getTransporterAdvancedStats(
            @PathVariable int id,
            @RequestParam(defaultValue = "6") int periodMonths) {
        return livraisonService.getTransporterAdvancedStats(id, periodMonths);
    }

    @GetMapping("/transporter/{id}/calendar")
    @Operation(description = "Calendrier mensuel des livraisons pour un transporteur")
    public List<Map<String, Object>> getTransporterCalendar(
            @PathVariable int id,
            @RequestParam int year,
            @RequestParam int month) {
        return livraisonService.getTransporterCalendar(id, year, month);
    }

    @GetMapping("/transporter/{id}/calendar-summary")
    @Operation(description = "Résumé statistique du calendrier mensuel")
    public Map<String, Object> getTransporterCalendarSummary(
            @PathVariable int id,
            @RequestParam int year,
            @RequestParam int month) {
        return livraisonService.getTransporterCalendarSummary(id, year, month);
    }

    @GetMapping("/transporter/{id}/groups")
    @Operation(description = "Groupes de livraison auxquels participe le transporteur")
    public List<Map<String, Object>> getTransporterGroups(@PathVariable int id) {
        return livraisonService.getTransporterGroups(id);
    }

    @GetMapping("/transporter/{id}/groups/{groupReference}")
    @Operation(description = "Détails d'un groupe de livraison spécifique")
    public Map<String, Object> getGroupDetails(
            @PathVariable int id,
            @PathVariable String groupReference) {
        return livraisonService.getGroupDetails(groupReference, id);
    }

    @PostMapping("/transporter/{id}/create-group")
    @Operation(description = "Créer un groupe à partir de livraisons sélectionnées")
    public Map<String, Object> createGroupFromDeliveries(
            @PathVariable int id,
            @RequestBody List<Integer> livraisonIds) {
        return livraisonService.createGroupFromDeliveries(id, livraisonIds);
    }

    @PutMapping("/transporter/{id}/groups/{groupReference}")
    @Operation(description = "Mettre à jour la composition d'un groupe")
    public Map<String, Object> updateGroupDeliveries(
            @PathVariable int id,
            @PathVariable String groupReference,
            @RequestBody List<Integer> livraisonIds) {
        return livraisonService.updateGroupDeliveries(id, groupReference, livraisonIds);
    }

    @DeleteMapping("/transporter/{id}/groups/{groupReference}")
    @Operation(description = "Supprimer un groupe de livraisons")
    public Map<String, Object> deleteGroup(
            @PathVariable int id,
            @PathVariable String groupReference) {
        return livraisonService.deleteGroup(id, groupReference);
    }

    @GetMapping("/transporter/{id}/history")
    @Operation(description = "Historique des livraisons terminees/annulees du transporteur")
    public List<Livraison> getTransporterHistory(@PathVariable int id) {
        return livraisonService.getTransporterHistory(id);
    }

    @GetMapping("/transporter/{id}/in-progress")
    @Operation(description = "Livraisons en cours/acceptees pour le transporteur")
    public List<Livraison> getTransporterInProgress(@PathVariable int id) {
        return livraisonService.getTransporterInProgress(id);
    }

    @GetMapping("/pending-farmer-requests")
    @Operation(description = "Demandes d'agriculteurs en attente de transporteur")
    public List<Livraison> getPendingFarmerRequests() {
        return livraisonService.getPendingFarmerRequestsForTransporter();
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    @GetMapping("/notifications/{userId}")
    @Operation(description = "Livraisons avec notifications pour un utilisateur")
    public List<Map<String, Object>> getNotifications(@PathVariable int userId) {
        return livraisonService.getNotificationsForUser(userId);
    }

    @PutMapping("/notifications/{id}/action")
    @Operation(description = "Agir sur une notification de livraison")
    public Map<String, Object> handleNotificationAction(
            @PathVariable int id,
            @RequestParam int actorId,
            @RequestParam String action,
            @RequestParam(required = false) Double counterPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime counterDateTime) {
        return livraisonService.handleNotificationAction(id, actorId, action, counterPrice, counterDateTime);
    }

    @GetMapping("/notifications/{userId}/count")
    @Operation(description = "Nombre de notifications non lues pour un utilisateur")
    public Map<String, Object> getUnreadNotificationsCount(@PathVariable int userId) {
        return livraisonService.getUnreadNotificationsCount(userId);
    }

    @PutMapping("/notifications/{id}/mark-read")
    @Operation(description = "Marquer une notification comme lue")
    public Map<String, Object> markNotificationAsRead(@PathVariable int id) {
        return livraisonService.handleNotificationAction(id, 0, "MARK_READ", null, null);
    }

    @PutMapping("/notifications/{userId}/mark-all-read")
    @Operation(description = "Marquer toutes les notifications d'un utilisateur comme lues")
    public Map<String, Object> markAllNotificationsAsRead(@PathVariable int userId) {
        return livraisonService.markAllNotificationsAsRead(userId);
    }

    @DeleteMapping("/notifications/{id}")
    @Operation(description = "Supprimer une notification")
    public Map<String, Object> deleteNotification(
            @PathVariable int id,
            @RequestParam int actorId) {
        return livraisonService.deleteNotification(id, actorId);
    }

    @GetMapping("/notifications/{userId}/negociation-pending")
    @Operation(description = "Livraisons avec négociation en attente pour un utilisateur")
    public List<Map<String, Object>> getPendingNegotiationNotifications(@PathVariable int userId) {
        return livraisonService.getPendingNegotiationNotifications(userId);
    }

    // ── Négociation avancée avec barre +/-5% ────────────────────────────────

    @GetMapping("/{id}/negociation-range")
    @Operation(description = "Récupérer la plage de négociation autorisée (+/-5%)")
    public Map<String, Object> getNegociationRange(@PathVariable Integer id) {
        return livraisonService.getNegociationRange(id);
    }

    @PostMapping("/{id}/negocier-barre")
    @Operation(description = "Négocier avec barre interactive (+/-5%)")
    public Map<String, Object> negocierAvecBarre(
            @PathVariable Integer id,
            @RequestBody tn.esprit.livraison.controller.dto.PriceProposalRequest request) {
        return livraisonService.negocierAvecBarre(id, request.actorId(), request.prixPropose(), request.proposedDateTime());
    }

    @PostMapping("/{id}/accepter-negociation-barre")
    @Operation(description = "Accepter une négociation depuis la barre")
    public Map<String, Object> accepterNegociationBarre(
            @PathVariable Integer id,
            @RequestParam int agriculteurId) {
        return livraisonService.accepterNegociationBarre(id, agriculteurId);
    }

    @PostMapping("/{id}/refuser-negociation-barre")
    @Operation(description = "Refuser une négociation depuis la barre")
    public Map<String, Object> refuserNegociationBarre(
            @PathVariable Integer id,
            @RequestParam int agriculteurId) {
        return livraisonService.refuserNegociationBarre(id, agriculteurId);
    }

    // ── Planification par l'agriculteur ─────────────────────────────────────

    @PostMapping("/{id}/planifier")
    @Operation(description = "Planifier une date de livraison par l'agriculteur")
    public Livraison planifierLivraison(
            @PathVariable Integer id,
            @RequestBody tn.esprit.livraison.controller.dto.ScheduleDeliveryRequest request) {
        return livraisonService.scheduleDeliveryDate(id, request.agriculteurId(), request.dateLivraisonPrevue());
    }

    @GetMapping("/agriculteur/{agriculteurId}/schedule")
    @Operation(description = "Calendrier des livraisons planifiées pour un agriculteur")
    public List<Map<String, Object>> getAgriculteurSchedule(
            @PathVariable int agriculteurId,
            @RequestParam int year,
            @RequestParam int month) {
        return livraisonService.getAgriculteurSchedule(agriculteurId, year, month);
    }

    @GetMapping("/agriculteur/{agriculteurId}/planning-stats")
    @Operation(description = "Statistiques de planification pour un agriculteur")
    public Map<String, Object> getAgriculteurPlanningStats(@PathVariable int agriculteurId) {
        return livraisonService.getAgriculteurPlanningStats(agriculteurId);
    }

    // ── Demandes en cours pour transporteur ───────────────────────────────────

    @GetMapping("/transporteur/{transporteurId}/demandes-en-cours")
    @Operation(description = "Voir les demandes en cours que le transporteur peut accepter")
    public List<Map<String, Object>> getDemandesEnCoursPourTransporteur(
            @PathVariable int transporteurId,
            @RequestParam(required = false) String status) {
        return livraisonService.getDemandesEnCoursPourTransporteur(transporteurId, status);
    }

    @GetMapping("/{id}/details-transporteur")
    @Operation(description = "Détails d'une livraison pour le transporteur")
    public Map<String, Object> getDetailsPourTransporteur(
            @PathVariable Integer id,
            @RequestParam int transporteurId) {
        return livraisonService.getDetailsPourTransporteur(id, transporteurId);
    }

}
