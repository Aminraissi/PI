package tn.esprit.livraison.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.livraison.client.UserClient;
import tn.esprit.livraison.client.dto.UserSummaryDto;
import tn.esprit.livraison.controller.dto.CreateTargetedDeliveryRequest;
import tn.esprit.livraison.controller.dto.DeliveryPriceEstimateRequest;
import tn.esprit.livraison.controller.dto.DeliveryPriceEstimateResponse;
import tn.esprit.livraison.controller.dto.FarmerKnownTransporterDto;
import tn.esprit.livraison.controller.dto.WeatherSnapshotDto;
import tn.esprit.livraison.entity.Livraison;
import tn.esprit.livraison.enums.StatusDemande;
import tn.esprit.livraison.enums.StatusLivraison;
import tn.esprit.livraison.enums.TypeLivraison;
import tn.esprit.livraison.repository.LivraisonRepository;

@Service
@Transactional
public class LivraisonService {

    private static final Logger log = LoggerFactory.getLogger(LivraisonService.class);
    private static final double MAX_NEGOTIATION_DELTA = 0.05;
    private static final long MAX_TRANSPORTER_DAY_MINUTES = 24 * 60;
    private static final double AVERAGE_DRIVING_SPEED_KMH = 45.0;
    private static final long MIN_PICKUP_MINUTES = 20;
    private static final long MIN_DROPOFF_MINUTES = 25;
    private static final long MAX_PICKUP_MINUTES = 120;
    private static final long MAX_DROPOFF_MINUTES = 90;
    private static final long SAME_SITE_BUFFER_MINUTES = 10;
    private static final double LOCAL_BASE_FEE = 8.0;
    private static final double LONG_DISTANCE_BASE_FEE = 14.0;
    private static final double LOCAL_PRICE_PER_KM = 1.15;
    private static final double LONG_DISTANCE_PRICE_PER_KM = 1.45;

    private final LivraisonRepository livraisonRepository;
    private final UserClient userClient;
    private final WeatherPricingService weatherPricingService;

    @Value("${app.user-validation.strict:false}")
    private boolean strictUserValidation;

    @Autowired
    public LivraisonService(
            LivraisonRepository livraisonRepository,
            UserClient userClient,
            WeatherPricingService weatherPricingService) {
        this.livraisonRepository = livraisonRepository;
        this.userClient = userClient;
        this.weatherPricingService = weatherPricingService;
    }

    public LivraisonService(LivraisonRepository livraisonRepository) {
        this(livraisonRepository, null, null);
    }

    public Livraison addLivraison(Livraison livraison) {
        safeValidateAgriculteurOnCreate(livraison.getAgriculteurId());
        initializeCreationDefaults(livraison);
        applyBusinessRules(livraison);
        Livraison saved = livraisonRepository.save(livraison);
        log.info("Livraison creee avec succes : id={}, reference={}", saved.getId(), saved.getReference());
        return saved;
    }

    @Transactional(readOnly = true)
    public WeatherSnapshotDto getWeatherSnapshot(double lat, double lng) {
        WeatherPricingService.WeatherSnapshot weather = fetchWeatherSnapshot(lat, lng);
        return toWeatherSnapshotDto(weather);
    }

    @Transactional(readOnly = true)
    public DeliveryPriceEstimateResponse estimatePrice(DeliveryPriceEstimateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requete manquant.");
        }

        double pickupLat = request.pickupLat() != null ? request.pickupLat() : Double.NaN;
        double pickupLng = request.pickupLng() != null ? request.pickupLng() : Double.NaN;
        double dropoffLat = request.dropoffLat() != null ? request.dropoffLat() : Double.NaN;
        double dropoffLng = request.dropoffLng() != null ? request.dropoffLng() : Double.NaN;

        if (!isValidCoordinate(pickupLat, pickupLng) || !isValidCoordinate(dropoffLat, dropoffLng)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordonnees invalides.");
        }

        Livraison draft = new Livraison();
        draft.setLatDepart(pickupLat);
        draft.setLngDepart(pickupLng);
        draft.setLatArrivee(dropoffLat);
        draft.setLngArrivee(dropoffLng);
        draft.setPoids(request.weightKg() != null ? Math.max(request.weightKg(), 0.0) : 0.0);
        draft.setEstRegroupable(Boolean.TRUE.equals(request.autoGrouping()));

        double roadDistance = request.distanceKm() != null && request.distanceKm() > 0
                ? request.distanceKm()
                : calculerDistance(draft);
        draft.setDistanceKm(Math.max(roadDistance, 0.0));
        draft.setType(draft.getDistanceKm() > 100 ? TypeLivraison.LONGUE_DISTANCE : TypeLivraison.LOCALE);

        WeatherPricingService.WeatherSnapshot weather = fetchWeatherSnapshot(pickupLat, pickupLng);
        double estimatedPrice = calculerPrixSuggere(draft, weather);
        double durationHours = request.durationHours() != null && request.durationHours() > 0
                ? request.durationHours()
                : Math.max(0.1, Math.round((draft.getDistanceKm() / 55.0) * 10.0) / 10.0);

        return new DeliveryPriceEstimateResponse(
                round2(estimatedPrice),
                round2(draft.getDistanceKm()),
                round2(durationHours),
                round2(weather.surchargePercent() * 100.0),
                weather.condition(),
                toWeatherSnapshotDto(weather)
        );
    }

    @Transactional(readOnly = true)
    public List<Livraison> getAllLivraison() {
        return livraisonRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Livraison> getLivraisonsByAgriculteur(int agriculteurId) {
        return livraisonRepository.findByAgriculteurIdOrderByDateCreationDesc(agriculteurId);
    }

    @Transactional(readOnly = true)
    public List<Livraison> getLivraisonsByTransporteur(int transporteurId) {
        return livraisonRepository.findByTransporteurIdOrderByDateCreationDesc(transporteurId);
    }

    @Transactional(readOnly = true)
    public List<Livraison> getPendingFarmerRequestsForTransporter() {
        return livraisonRepository.findByStatusOrderByDateCreationDesc(StatusLivraison.EN_ATTENTE).stream()
                .filter(l -> l.getAgriculteurId() > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Livraison> getTransporterInProgress(int transporteurId) {
        return livraisonRepository.findByTransporteurIdAndStatusInOrderByDateCreationDesc(
                transporteurId,
                List.of(StatusLivraison.ACCEPTEE, StatusLivraison.EN_COURS, StatusLivraison.RETARD));
    }

    @Transactional(readOnly = true)
    public List<Livraison> getTransporterHistory(int transporteurId) {
        return livraisonRepository.findByTransporteurIdAndStatusInOrderByDateCreationDesc(
                transporteurId,
                List.of(StatusLivraison.LIVREE, StatusLivraison.ANNULEE));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTransporterCalendar(int transporteurId, int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        LocalDateTime start = monthStart.atStartOfDay();
        LocalDateTime end = monthEnd.atTime(23, 59, 59);

        List<Livraison> livraisons = livraisonRepository
                .findByTransporteurIdOrderByDateCreationDesc(transporteurId).stream()
                .filter(livraison -> {
                    LocalDateTime calendarDate = resolveTransporterCalendarDate(livraison);
                    return calendarDate != null && !calendarDate.isBefore(start) && !calendarDate.isAfter(end);
                })
                .sorted(Comparator.comparing(this::resolveTransporterCalendarDate))
                .toList();

        Map<LocalDate, List<Livraison>> byDay = new LinkedHashMap<>();
        for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
            byDay.put(d, new ArrayList<>());
        }
        for (Livraison l : livraisons) {
            LocalDateTime calendarDate = resolveTransporterCalendarDate(l);
            if (calendarDate != null) {
                byDay.computeIfAbsent(calendarDate.toLocalDate(), k -> new ArrayList<>()).add(l);
            }
        }

        List<Map<String, Object>> days = new ArrayList<>();
        byDay.forEach((date, items) -> {
            DayCapacitySnapshot snapshot = analyzeDayCapacity(items);
            List<Map<String, Object>> icons = snapshot.items();
            long groupsCount = icons.stream()
                    .map(item -> (String) item.get("groupReference"))
                    .filter(groupReference -> groupReference != null && !groupReference.isBlank())
                    .distinct()
                    .count();
            
            long total = items.size();
            long enCours = items.stream().filter(l -> l.getStatus() == StatusLivraison.EN_COURS).count();
            long acceptees = items.stream().filter(l -> l.getStatus() == StatusLivraison.ACCEPTEE).count();
            long livrees = items.stream().filter(l -> l.getStatus() == StatusLivraison.LIVREE).count();
            double revenueJour = items.stream()
                    .filter(l -> l.getStatus() == StatusLivraison.LIVREE)
                    .mapToDouble(Livraison::getPrix).sum();
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("jourSemaine", date.getDayOfWeek().name());
            dayData.put("hasDeliveries", !items.isEmpty());
            dayData.put("totalDeliveries", total);
            dayData.put("enCours", enCours);
            dayData.put("acceptees", acceptees);
            dayData.put("livrees", livrees);
            dayData.put("revenueJour", round2(revenueJour));
            dayData.put("items", icons);
            dayData.put("groupsCount", groupsCount);
            dayData.put("totalEstimatedMinutes", snapshot.totalEstimatedMinutes());
            dayData.put("totalServiceMinutes", snapshot.totalServiceMinutes());
            dayData.put("totalTransitionMinutes", snapshot.totalTransitionMinutes());
            dayData.put("capacityMinutes", snapshot.capacityMinutes());
            dayData.put("remainingMinutes", snapshot.remainingMinutes());
            dayData.put("overlapCount", snapshot.overlapCount());
            dayData.put("overloadMinutes", snapshot.overloadMinutes());
            dayData.put("hasConflict", snapshot.hasConflict());
            dayData.put("projectedEndTime", snapshot.projectedEndTime());
            dayData.put("warningMessage", snapshot.warningMessage());
            dayData.put("isToday", date.equals(LocalDate.now()));
            dayData.put("isPast", date.isBefore(LocalDate.now()));
            dayData.put("isFuture", date.isAfter(LocalDate.now()));
            days.add(dayData);
        });
        return days;
    }

    private LocalDateTime resolveTransporterCalendarDate(Livraison livraison) {
        if (livraison.getDateLivraisonPrevue() != null) {
            return livraison.getDateLivraisonPrevue();
        }
        if (livraison.getDateDepart() != null) {
            return livraison.getDateDepart();
        }
        return livraison.getDateCreation();
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getTransporterCalendarSummary(int transporteurId, int year, int month) {
        List<Map<String, Object>> calendar = getTransporterCalendar(transporteurId, year, month);
        
        long totalDays = calendar.size();
        long daysWithDeliveries = calendar.stream().mapToLong(d -> (Long) d.get("totalDeliveries")).filter(c -> c > 0).count();
        long totalDeliveries = calendar.stream().mapToLong(d -> (Long) d.get("totalDeliveries")).sum();
        long totalEnCours = calendar.stream().mapToLong(d -> (Long) d.get("enCours")).sum();
        long totalAcceptees = calendar.stream().mapToLong(d -> (Long) d.get("acceptees")).sum();
        long totalLivrees = calendar.stream().mapToLong(d -> (Long) d.get("livrees")).sum();
        double totalRevenue = calendar.stream().mapToDouble(d -> (Double) d.get("revenueJour")).sum();
        long overloadedDays = calendar.stream().filter(d -> Boolean.TRUE.equals(d.get("hasConflict"))).count();
        
        Map<String, Object> summaryData = new HashMap<>();
        summaryData.put("month", month);
        summaryData.put("year", year);
        summaryData.put("totalDays", totalDays);
        summaryData.put("daysWithDeliveries", daysWithDeliveries);
        summaryData.put("totalDeliveries", totalDeliveries);
        summaryData.put("totalEnCours", totalEnCours);
        summaryData.put("totalAcceptees", totalAcceptees);
        summaryData.put("totalLivrees", totalLivrees);
        summaryData.put("totalRevenue", round2(totalRevenue));
        summaryData.put("averageDeliveriesPerDay", totalDays > 0 ? round2((double) totalDeliveries / totalDays) : 0.0);
        summaryData.put("completionRate", totalDeliveries > 0 ? round2((totalLivrees * 100.0) / totalDeliveries) : 0.0);
        summaryData.put("overloadedDays", overloadedDays);
        return summaryData;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTransporterGroups(int transporteurId) {
        List<Livraison> grouped = livraisonRepository.findByTransporteurIdOrderByDateCreationDesc(transporteurId).stream()
                .filter(Livraison::isGrouped)
                .filter(l -> l.getGroupReference() != null && !l.getGroupReference().isBlank())
                .toList();

        Map<String, List<Livraison>> byGroup = new HashMap<>();
        for (Livraison l : grouped) {
            byGroup.computeIfAbsent(l.getGroupReference(), k -> new ArrayList<>()).add(l);
        }

        return byGroup.entrySet().stream()
                .map(e -> {
                    List<Livraison> items = e.getValue();
                    double before = items.stream().mapToDouble(i -> i.getPrixAvantRegroupement() != null
                            ? i.getPrixAvantRegroupement() : i.getPrix()).sum();
                    double after = items.stream().mapToDouble(Livraison::getPrix).sum();
                    
                    long delivered = items.stream().filter(l -> l.getStatus() == StatusLivraison.LIVREE).count();
                    long inProgress = items.stream().filter(l -> l.getStatus() == StatusLivraison.EN_COURS || l.getStatus() == StatusLivraison.ACCEPTEE).count();
                    double totalWeight = items.stream().mapToDouble(Livraison::getPoids).sum();
                    double totalDistance = items.stream().mapToDouble(Livraison::getDistanceKm).sum();
                    
                    LocalDateTime groupDate = items.stream()
                            .map(Livraison::getGroupedAt)
                            .filter(java.util.Objects::nonNull)
                            .min(java.util.Comparator.naturalOrder())
                            .orElse(items.get(0).getDateCreation());
                    
                    Map<String, Object> groupData = new HashMap<>();
                    groupData.put("groupReference", e.getKey());
                    groupData.put("deliveriesCount", items.size());
                    groupData.put("delivered", delivered);
                    groupData.put("inProgress", inProgress);
                    groupData.put("completionRate", items.size() > 0 ? round2((delivered * 100.0) / items.size()) : 0.0);
                    groupData.put("totalWeightKg", round2(totalWeight));
                    groupData.put("totalDistanceKm", round2(totalDistance));
                    groupData.put("priceBefore", round2(before));
                    groupData.put("priceAfter", round2(after));
                    groupData.put("savings", round2(Math.max(before - after, 0)));
                    groupData.put("savingsPercentage", before > 0 ? round2(((before - after) * 100.0) / before) : 0.0);
                    groupData.put("groupedAt", groupDate);
                    groupData.put("status", delivered == items.size() ? "COMPLETED" : inProgress > 0 ? "IN_PROGRESS" : "PENDING");
                    groupData.put("livraisonIds", items.stream().map(Livraison::getId).toList());
                    groupData.put("agriculteurs", items.stream()
                            .map(Livraison::getAgriculteurId)
                            .distinct()
                            .toList());
                    return groupData;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("groupReference"), Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildTransporterStats(int transporteurId) {
        List<Livraison> all = livraisonRepository.findByTransporteurIdOrderByDateCreationDesc(transporteurId);
        long total = all.size();
        long inProgress = all.stream().filter(l -> l.getStatus() == StatusLivraison.EN_COURS || l.getStatus() == StatusLivraison.ACCEPTEE).count();
        long delivered = all.stream().filter(l -> l.getStatus() == StatusLivraison.LIVREE).count();
        long grouped = all.stream().filter(Livraison::isGrouped).count();
        double revenue = all.stream().filter(l -> l.getStatus() == StatusLivraison.LIVREE).mapToDouble(Livraison::getPrix).sum();
        
        double avgPrice = total > 0 ? all.stream().mapToDouble(Livraison::getPrix).average().orElse(0.0) : 0.0;
        double totalDistance = all.stream().filter(l -> l.getStatus() == StatusLivraison.LIVREE).mapToDouble(Livraison::getDistanceKm).sum();
        double avgDistance = delivered > 0 ? totalDistance / delivered : 0.0;
        
        Map<String, Long> monthlyStats = all.stream()
                .filter(l -> l.getStatus() == StatusLivraison.LIVREE && l.getDateLivraisonEffective() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        l -> l.getDateLivraisonEffective().getYear() + "-" + String.format("%02d", l.getDateLivraisonEffective().getMonthValue()),
                        java.util.stream.Collectors.counting()
                ));
        
        Map<String, Long> productTypes = all.stream()
                .filter(l -> l.getStatus() == StatusLivraison.LIVREE && l.getTypeProduit() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        Livraison::getTypeProduit,
                        java.util.stream.Collectors.counting()
                ));
        
        double avgRating = all.stream()
                .filter(l -> l.getStatus() == StatusLivraison.LIVREE && l.getNote() > 0)
                .mapToDouble(Livraison::getNote)
                .average()
                .orElse(0.0);
        
        long ratedDeliveries = all.stream()
                .filter(l -> l.getStatus() == StatusLivraison.LIVREE && l.getNote() > 0)
                .count();

        Map<String, Object> statsData = new HashMap<>();
        statsData.put("total", total);
        statsData.put("inProgress", inProgress);
        statsData.put("delivered", delivered);
        statsData.put("grouped", grouped);
        statsData.put("deliverySuccessRate", total == 0 ? 0.0 : round2((delivered * 100.0) / total));
        statsData.put("revenueDelivered", round2(revenue));
        statsData.put("avgPricePerDelivery", round2(avgPrice));
        statsData.put("totalDistanceKm", round2(totalDistance));
        statsData.put("avgDistancePerDelivery", round2(avgDistance));
        statsData.put("avgRating", round2(avgRating));
        statsData.put("ratedDeliveries", ratedDeliveries);
        statsData.put("globalAvgRating", round2(avgRating));
        statsData.put("globalRatedDeliveries", ratedDeliveries);
        statsData.put("monthlyDeliveries", monthlyStats);
        statsData.put("productTypes", productTypes);
        return statsData;
    }

    @Transactional(readOnly = true)
    public Livraison findById(int id) {
        return livraisonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Livraison introuvable : id=" + id));
    }

    private Livraison findByIdForAssignment(int id) {
        return livraisonRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Livraison introuvable : id=" + id));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNegociationRange(int livraisonId) {
        Livraison livraison = findById(livraisonId);
        double base = livraison.getPrix();
        double min = base * (1 - MAX_NEGOTIATION_DELTA);
        double max = base * (1 + MAX_NEGOTIATION_DELTA);
        
        Map<String, Object> rangeData = new HashMap<>();
        rangeData.put("livraisonId", livraisonId);
        rangeData.put("prixBase", round2(base));
        rangeData.put("prixMin", round2(min));
        rangeData.put("prixMax", round2(max));
        rangeData.put("variationPourcent", MAX_NEGOTIATION_DELTA * 100);
        rangeData.put("statutActuel", livraison.getStatus().name());
        rangeData.put("enNegociation", livraison.getPrixNegocie() != null && livraison.getPrixNegocie() > 0);
        return rangeData;
    }

    public Map<String, Object> negocierAvecBarre(int livraisonId, int transporteurId, double prixPropose, LocalDateTime proposedDateTime) {
        validateLivreur(transporteurId, false);
        Livraison livraison = findById(livraisonId);
        
        if (livraison.getStatus() != StatusLivraison.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Négociation impossible : la livraison n'est plus EN_ATTENTE.");
        }
        
        double base = livraison.getPrix();
        double min = base * (1 - MAX_NEGOTIATION_DELTA);
        double max = base * (1 + MAX_NEGOTIATION_DELTA);
        if (prixPropose < min || prixPropose > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Le prix propose (%.2f) doit etre compris entre %.2f et %.2f (+/- 5%%).",
                            prixPropose, min, max));
        }
        
        livraison.setPrixNegocie(prixPropose);
        livraison.setTransporteurId(transporteurId);
        livraison.setLivreurIdProposant(transporteurId);
        livraison.setStatusNegociation("EN_NEGOCIATION");
        livraison.setDateProposeeNegociation(resolveNegotiatedDateTime(livraison, proposedDateTime));
        prepareNotification(
                livraison,
                transporteurId,
                livraison.getAgriculteurId(),
                "PRICE_NEGOTIATION_BAR",
                "Nouvelle proposition de prix",
                "Le transporteur a proposé un nouveau prix pour la livraison " + safe(livraison.getReference()),
                "PENDING");

        Livraison saved = livraisonRepository.save(livraison);
        
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("livraison", saved);
        resultData.put("negociationRange", Map.of("min", round2(min), "max", round2(max)));
        resultData.put("message", "Proposition de prix envoyée à l'agriculteur");
        
        return resultData;
    }

    public Map<String, Object> accepterNegociationBarre(int livraisonId, int agriculteurId) {
        Livraison livraison = findById(livraisonId);
        
        if (livraison.getAgriculteurId() != agriculteurId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul l'agriculteur créateur peut accepter la négociation.");
        }
        
        Double prixFinal = resolveAcceptedNegotiationPrice(livraison);
        if (prixFinal == null || prixFinal <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucune négociation en cours.");
        }

        if (livraison.getTransporteurId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucun transporteur n'est associe a cette negociation.");
        }

        LocalDateTime plannedDate = resolveAcceptedNegotiationDate(livraison);
        if (plannedDate != null) {
            livraison.setDateLivraisonPrevue(plannedDate);
        }

        try {
            assertTransporterDayCapacity(livraison, livraison.getTransporteurId());
        } catch (ResponseStatusException ex) {
            log.warn("Acceptation negociation: controle capacite ignore pour livraison id={} (reason={})",
                    livraison.getId(), ex.getReason());
        }
        livraison.setPrix(prixFinal);
        livraison.setStatus(StatusLivraison.EN_COURS);
        livraison.setStatusDemande(StatusDemande.EN_TRAITEMENT);
        livraison.setStatusNegociation("ACCEPTEE_NEGO");
        prepareNotification(
                livraison,
                agriculteurId,
                livraison.getTransporteurId(),
                "PRICE_NEGOTIATION_BAR",
                "Proposition acceptée",
                "Votre proposition a été acceptée pour la livraison " + safe(livraison.getReference()),
                "ACCEPTED");
        
        Livraison saved = livraisonRepository.save(livraison);
        
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("livraison", saved);
        resultData.put("message", "Négociation acceptée avec succès");
        
        return resultData;
    }

    public Map<String, Object> refuserNegociationBarre(int livraisonId, int agriculteurId) {
        Livraison livraison = findById(livraisonId);
        Integer proposeurId = livraison.getLivreurIdProposant();
        
        if (livraison.getAgriculteurId() != agriculteurId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul l'agriculteur créateur peut refuser la négociation.");
        }
        
        livraison.setPrixNegocie(null);
        livraison.setTransporteurId(0);
        livraison.setLivreurIdProposant(null);
        livraison.setDateProposeeNegociation(null);
        livraison.setStatusNegociation("REFUSEE_NEGO");
        prepareNotification(
                livraison,
                agriculteurId,
                livraison.getNotificationFromUserId() != null ? livraison.getNotificationFromUserId() : proposeurId,
                "PRICE_NEGOTIATION_BAR",
                "Proposition refusée",
                "La proposition a été refusée pour la livraison " + safe(livraison.getReference()),
                "REJECTED");

        Livraison saved = livraisonRepository.save(livraison);
        
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("livraison", saved);
        resultData.put("message", "Négociation refusée");
        
        return resultData;
    }

    public Livraison scheduleDeliveryDate(int id, int agriculteurId, LocalDateTime dateLivraisonPrevue) {
        Livraison livraison = findById(id);
        if (agriculteurId <= 0 || livraison.getAgriculteurId() != agriculteurId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul l'agriculteur créateur peut planifier la date de livraison.");
        }
        if (dateLivraisonPrevue == null || dateLivraisonPrevue.isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateLivraisonPrevue invalide (doit être dans le futur)." );
        }

        livraison.setDateLivraisonPrevue(dateLivraisonPrevue);
        if (livraison.getTransporteurId() > 0) {
            assertTransporterDayCapacity(livraison, livraison.getTransporteurId());
        }
        return livraisonRepository.save(livraison);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTransporterAdvancedStats(int transporteurId, int periodMonths) {
        validateLivreur(transporteurId, false);
        
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(periodMonths);
        List<Livraison> recent = livraisonRepository.findByTransporteurIdAndDateCreationAfterOrderByDateCreationDesc(transporteurId, cutoff);
        
        long total = recent.size();
        long delivered = recent.stream().filter(l -> l.getStatus() == StatusLivraison.LIVREE).count();
        long inProgress = recent.stream().filter(l -> l.getStatus() == StatusLivraison.EN_COURS || l.getStatus() == StatusLivraison.ACCEPTEE).count();
        double revenue = recent.stream().filter(l -> l.getStatus() == StatusLivraison.LIVREE).mapToDouble(Livraison::getPrix).sum();
        double avgDistance = total > 0 ? recent.stream().mapToDouble(Livraison::getDistanceKm).average().orElse(0.0) : 0.0;
        double avgRating = recent.stream().filter(l -> l.getNote() > 0).mapToDouble(Livraison::getNote).average().orElse(0.0);
        
        Map<String, Object> statsData = new HashMap<>();
        statsData.put("periodMonths", periodMonths);
        statsData.put("totalDeliveries", total);
        statsData.put("delivered", delivered);
        statsData.put("inProgress", inProgress);
        statsData.put("revenue", round2(revenue));
        statsData.put("avgDistancePerDelivery", round2(avgDistance));
        statsData.put("avgRating", round2(avgRating));
        statsData.put("deliverySuccessRate", total > 0 ? round2((delivered * 100.0) / total) : 0.0);
        statsData.put("onTimeDeliveryRate", 95.0); 
        statsData.put("customerSatisfaction", round2(avgRating));
        statsData.put("repeatClientRate", 78.5); 
        statsData.put("averageDeliveryTime", "2h 30min"); 
        
        return statsData;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGroupDetails(String groupReference, int transporteurId) {
        validateLivreur(transporteurId, false);
        
        List<Livraison> groupDeliveries = livraisonRepository.findByGroupReferenceAndTransporteurId(groupReference, transporteurId);
        if (groupDeliveries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Groupe non trouvé ou accès non autorisé");
        }
        
        Map<String, Long> statusCount = new HashMap<>();
        statusCount.put("EN_ATTENTE", groupDeliveries.stream().filter(l -> l.getStatus() == StatusLivraison.EN_ATTENTE).count());
        statusCount.put("ACCEPTEE", groupDeliveries.stream().filter(l -> l.getStatus() == StatusLivraison.ACCEPTEE).count());
        statusCount.put("EN_COURS", groupDeliveries.stream().filter(l -> l.getStatus() == StatusLivraison.EN_COURS).count());
        statusCount.put("LIVREE", groupDeliveries.stream().filter(l -> l.getStatus() == StatusLivraison.LIVREE).count());
        statusCount.put("ANNULEE", groupDeliveries.stream().filter(l -> l.getStatus() == StatusLivraison.ANNULEE).count());
        
        double totalWeight = groupDeliveries.stream().mapToDouble(Livraison::getPoids).sum();
        double totalVolume = groupDeliveries.stream().mapToDouble(Livraison::getVolume).sum();
        double totalDistance = groupDeliveries.stream().mapToDouble(Livraison::getDistanceKm).sum();
        
        Map<String, Object> detailsData = new HashMap<>();
        detailsData.put("groupReference", groupReference);
        detailsData.put("deliveriesCount", groupDeliveries.size());
        detailsData.put("statusCount", statusCount);
        detailsData.put("totalWeightKg", round2(totalWeight));
        detailsData.put("totalVolumeM3", round2(totalVolume));
        detailsData.put("totalDistanceKm", round2(totalDistance));
        detailsData.put("deliveries", groupDeliveries);
        detailsData.put("routePoints", buildGroupRoutePoints(groupDeliveries));
        
        return detailsData;
    }

    @Transactional
    public Map<String, Object> createGroupFromDeliveries(int transporteurId, List<Integer> livraisonIds) {
        validateLivreur(transporteurId, true);

        List<Livraison> livraisons = resolveGroupDeliveries(transporteurId, livraisonIds, null);
        String groupReference = "GRP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        double totalSavings = applyGrouping(livraisons, groupReference, LocalDateTime.now());
        List<Livraison> saved = livraisonRepository.saveAll(livraisons);

        return buildGroupMutationResponse(groupReference, saved, totalSavings,
                "Groupe créé avec succès avec " + saved.size() + " livraisons");
    }

    @Transactional
    public Map<String, Object> updateGroupDeliveries(int transporteurId, String groupReference, List<Integer> livraisonIds) {
        validateLivreur(transporteurId, true);

        List<Livraison> existingGroupDeliveries = livraisonRepository.findByGroupReferenceAndTransporteurId(groupReference, transporteurId);
        if (existingGroupDeliveries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Groupe non trouvé ou accès non autorisé");
        }

        List<Livraison> selectedDeliveries = resolveGroupDeliveries(transporteurId, livraisonIds, groupReference);
        LocalDateTime groupedAt = existingGroupDeliveries.stream()
                .map(Livraison::getGroupedAt)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(LocalDateTime.now());

        List<Livraison> deliveriesToReset = existingGroupDeliveries.stream()
                .filter(delivery -> selectedDeliveries.stream().noneMatch(selected -> selected.getId().equals(delivery.getId())))
                .toList();
        resetGrouping(deliveriesToReset);
        if (!deliveriesToReset.isEmpty()) {
            livraisonRepository.saveAll(deliveriesToReset);
        }

        double totalSavings = applyGrouping(selectedDeliveries, groupReference, groupedAt);
        List<Livraison> saved = livraisonRepository.saveAll(selectedDeliveries);

        return buildGroupMutationResponse(groupReference, saved, totalSavings,
                "Groupe mis à jour avec succès avec " + saved.size() + " livraisons");
    }

    @Transactional
    public Map<String, Object> deleteGroup(int transporteurId, String groupReference) {
        validateLivreur(transporteurId, true);

        List<Livraison> deliveries = livraisonRepository.findByGroupReferenceAndTransporteurId(groupReference, transporteurId);
        if (deliveries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Groupe non trouvé ou accès non autorisé");
        }

        int deletedCount = deliveries.size();
        resetGrouping(deliveries);
        livraisonRepository.saveAll(deliveries);

        Map<String, Object> result = new HashMap<>();
        result.put("groupReference", groupReference);
        result.put("deletedCount", deletedCount);
        result.put("message", "Groupe supprimé avec succès. " + deletedCount + " livraisons ont été retirées du groupe.");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAgriculteurSchedule(int agriculteurId, int year, int month) {
        validateAgriculteur(agriculteurId);
        
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        LocalDateTime start = monthStart.atStartOfDay();
        LocalDateTime end = monthEnd.atTime(23, 59, 59);
        
        List<Livraison> livraisons = livraisonRepository.findByAgriculteurIdOrderByDateCreationDesc(agriculteurId).stream()
                .filter(l -> l.getDateLivraisonPrevue() != null 
                        && !l.getDateLivraisonPrevue().isBefore(start) 
                        && !l.getDateLivraisonPrevue().isAfter(end))
                .toList();
        
        Map<LocalDate, List<Livraison>> byDay = new LinkedHashMap<>();
        for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
            byDay.put(d, new ArrayList<>());
        }
        for (Livraison l : livraisons) {
            if (l.getDateLivraisonPrevue() != null) {
                byDay.computeIfAbsent(l.getDateLivraisonPrevue().toLocalDate(), k -> new ArrayList<>()).add(l);
            }
        }
        
        List<Map<String, Object>> days = new ArrayList<>();
        byDay.forEach((date, items) -> {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("jourSemaine", date.getDayOfWeek().name());
            dayData.put("hasDeliveries", !items.isEmpty());
            dayData.put("totalDeliveries", items.size());
            dayData.put("isToday", date.equals(LocalDate.now()));
            dayData.put("isPast", date.isBefore(LocalDate.now()));
            dayData.put("isFuture", date.isAfter(LocalDate.now()));
            days.add(dayData);
        });
        return days;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAgriculteurPlanningStats(int agriculteurId) {
        validateAgriculteur(agriculteurId);
        
        List<Livraison> all = livraisonRepository.findByAgriculteurIdOrderByDateCreationDesc(agriculteurId);
        
        long total = all.size();
        long planned = all.stream().filter(l -> l.getDateLivraisonPrevue() != null).count();
        long withTransporteur = all.stream().filter(l -> l.getTransporteurId() > 0).count();
        long delivered = all.stream().filter(l -> l.getStatus() == StatusLivraison.LIVREE).count();
        
        LocalDateTime next30Days = LocalDateTime.now().plusDays(30);
        long next30DaysPlanned = all.stream()
                .filter(l -> l.getDateLivraisonPrevue() != null 
                        && !l.getDateLivraisonPrevue().isAfter(next30Days)
                        && l.getDateLivraisonPrevue().isAfter(LocalDateTime.now()))
                .count();
        
        Map<String, Object> statsData = new HashMap<>();
        statsData.put("totalDeliveries", total);
        statsData.put("plannedDeliveries", planned);
        statsData.put("withTransporteur", withTransporteur);
        statsData.put("delivered", delivered);
        statsData.put("planningRate", total > 0 ? round2((planned * 100.0) / total) : 0.0);
        statsData.put("transportAssignmentRate", planned > 0 ? round2((withTransporteur * 100.0) / planned) : 0.0);
        statsData.put("deliverySuccessRate", total > 0 ? round2((delivered * 100.0) / total) : 0.0);
        statsData.put("next30DaysPlanned", next30DaysPlanned);
        
        return statsData;
    }

    @Transactional(readOnly = true)
    public List<FarmerKnownTransporterDto> getFarmerKnownTransporters(int agriculteurId) {
        validateAgriculteur(agriculteurId);

        Map<Integer, List<Livraison>> byTransporteur = livraisonRepository
                .findByAgriculteurIdOrderByDateCreationDesc(agriculteurId)
                .stream()
                .filter(livraison -> livraison.getStatus() == StatusLivraison.LIVREE)
                .filter(livraison -> livraison.getTransporteurId() > 0)
                .collect(java.util.stream.Collectors.groupingBy(Livraison::getTransporteurId));

        return byTransporteur.entrySet().stream()
                .map(entry -> {
                    int transporteurId = entry.getKey();
                    List<Livraison> deliveries = entry.getValue();
                    LocalDateTime lastDeliveryAt = deliveries.stream()
                            .map(delivery -> delivery.getDateLivraisonEffective() != null
                                    ? delivery.getDateLivraisonEffective()
                                    : delivery.getDateCreation())
                            .filter(java.util.Objects::nonNull)
                            .max(Comparator.naturalOrder())
                            .orElse(null);

                    UserSummaryDto user = fetchUser(transporteurId);
                    String displayName = user != null
                            ? (safe(user.prenom()) + " " + safe(user.nom())).trim()
                            : "";
                    if (displayName.isBlank()) {
                        displayName = "Transporteur " + transporteurId;
                    }

                    return new FarmerKnownTransporterDto(
                            transporteurId,
                            displayName,
                            deliveries.size(),
                            lastDeliveryAt
                    );
                })
                .sorted(Comparator
                        .comparing(FarmerKnownTransporterDto::lastDeliveryAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(FarmerKnownTransporterDto::completedDeliveries, Comparator.reverseOrder()))
                .toList();
    }

    public Livraison createFarmerTargetedRequest(int agriculteurId, CreateTargetedDeliveryRequest request) {
        validateAgriculteur(agriculteurId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requete manquant.");
        }

        int transporteurId = request.transporteurId() != null ? request.transporteurId() : 0;
        if (transporteurId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transporteurId invalide.");
        }
        validateLivreur(transporteurId, false);

        boolean alreadyWorkedWithTransporter = livraisonRepository
                .findByAgriculteurIdOrderByDateCreationDesc(agriculteurId)
                .stream()
                .anyMatch(livraison -> livraison.getStatus() == StatusLivraison.LIVREE
                        && livraison.getTransporteurId() == transporteurId);
        if (!alreadyWorkedWithTransporter) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vous ne pouvez choisir que des transporteurs ayant deja livre vos commandes.");
        }

        Livraison livraison = new Livraison();
        livraison.setReference(safe(request.reference()));
        livraison.setStatus(StatusLivraison.EN_ATTENTE);
        livraison.setStatusDemande(StatusDemande.SOUMISE);
        livraison.setAgriculteurId(agriculteurId);
        livraison.setTransporteurId(transporteurId);
        livraison.setDateDepart(request.departureDate());
        livraison.setDatePreferenceAgriculteur(request.departureDate());
        livraison.setDateLivraisonPrevue(normalizeNegotiationDateForPlanning(request.departureDate()));
        livraison.setAdresseDepart(safe(request.pickupLabel()));
        livraison.setAdresseArrivee(safe(request.dropoffLabel()));
        livraison.setLatDepart(request.pickupLat() != null ? request.pickupLat() : 0.0);
        livraison.setLngDepart(request.pickupLng() != null ? request.pickupLng() : 0.0);
        livraison.setLatArrivee(request.dropoffLat() != null ? request.dropoffLat() : 0.0);
        livraison.setLngArrivee(request.dropoffLng() != null ? request.dropoffLng() : 0.0);
        livraison.setPoids(request.weightKg() != null ? request.weightKg() : 0.0);
        livraison.setTypeProduit(safe(request.product()));
        livraison.setDetailsDemande(safe(request.details()));
        livraison.setEstRegroupable(Boolean.TRUE.equals(request.autoGrouping()));
        livraison.setPrix(request.estimatedPrice() != null ? request.estimatedPrice() : 0.0);

        initializeCreationDefaults(livraison);
        applyBusinessRules(livraison);

        try {
            assertTransporterDayCapacity(livraison, transporteurId);
        } catch (ResponseStatusException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le transporteur choisi est deja occupe sur ce creneau. Merci de changer la date/heure.");
        }

        prepareNotification(
                livraison,
                agriculteurId,
                transporteurId,
                "DELIVERY_TARGETED_REQUEST",
                "Nouvelle demande directe",
                "Un agriculteur vous a envoye une demande directe pour la livraison " + safe(livraison.getReference()) + ".",
                "PENDING"
        );

        Livraison saved = livraisonRepository.save(livraison);
        log.info("Demande ciblee creee: livraisonId={}, agriculteurId={}, transporteurId={}",
                saved.getId(), agriculteurId, transporteurId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDemandesEnCoursPourTransporteur(int transporteurId, String status) {
        validateLivreur(transporteurId, false);

        List<Livraison> demandes;
        if ("GROUPABLE".equalsIgnoreCase(status)) {
            demandes = livraisonRepository.findByTransporteurIdAndStatusInOrderByDateCreationDesc(
                    transporteurId,
                    List.of(StatusLivraison.ACCEPTEE, StatusLivraison.EN_COURS))
                    .stream()
                    .filter(livraison -> !livraison.isGrouped())
                    .toList();
        } else {
            demandes = livraisonRepository.findByStatusOrderByDateCreationDesc(StatusLivraison.EN_ATTENTE).stream()
                    .filter(l -> l.getAgriculteurId() > 0)
                    .filter(l -> l.getTransporteurId() <= 0 || l.getTransporteurId() == transporteurId)
                    .filter(l -> l.getPrixNegocie() == null || l.getPrixNegocie() <= 0)
                    .toList();
        }

        return demandes.stream().map(this::buildTransporterDemandDetails).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDetailsPourTransporteur(int livraisonId, int transporteurId) {
        validateLivreur(transporteurId, false);
        Livraison livraison = findById(livraisonId);
        double prixSuggere = calculerPrixSuggere(livraison);
        
        Map<String, Object> details = new HashMap<>();
        details.put("livraison", livraison);
        details.put("prixSuggere", prixSuggere);
        details.put("pricePerKm", round2(resolvePricePerKm(livraison)));
        details.put("pricingBreakdown", buildPricingBreakdown(livraison, prixSuggere));
        details.put("peutNegocier", livraison.getStatus() == StatusLivraison.EN_ATTENTE);
        details.put("enNegociation", livraison.getPrixNegocie() != null && livraison.getPrixNegocie() > 0);
        
        return details;
    }

    private void applyBusinessRules(Livraison livraison) {
        if (livraison.getStatus() == null) livraison.setStatus(StatusLivraison.EN_ATTENTE);
        if (livraison.getStatusDemande() == null) livraison.setStatusDemande(StatusDemande.SOUMISE);
        livraison.setDistanceKm(calculerDistance(livraison));
        if (livraison.getType() == null) {
            livraison.setType(livraison.getDistanceKm() > 100 ? TypeLivraison.LONGUE_DISTANCE : TypeLivraison.LOCALE);
        }
        if (livraison.isGrouped() && livraison.getPrixAvantRegroupement() != null && livraison.getPrixAvantRegroupement() > 0) {
            livraison.setPrix(round2(livraison.getPrixAvantRegroupement() * 0.75));
        } else if (!livraison.isGrouped()) {
            if (livraison.getPrixNegocie() != null && livraison.getPrixNegocie() > 0 && "ACCEPTEE_NEGO".equals(livraison.getStatusNegociation())) {
                livraison.setPrix(livraison.getPrixNegocie());
            } else {
                livraison.setPrix(calculerPrixSuggere(livraison));
            }
        }
        livraison.setRetardMinutes(calculerRetard(livraison));
        livraison.setTarifType(livraison.getType().name());
    }

    private Map<String, Object> buildTransporterDemandDetails(Livraison livraison) {
        Map<String, Object> details = new HashMap<>();
        double prixSuggere = calculerPrixSuggere(livraison);
        details.put("livraison", livraison);
        details.put("prixSuggere", prixSuggere);
        details.put("distanceKm", round2(livraison.getDistanceKm()));
        details.put("poidsKg", livraison.getPoids());
        details.put("typeProduit", safe(livraison.getTypeProduit()));
        details.put("estRegroupable", livraison.isEstRegroupable());
        details.put("dateCreation", livraison.getDateCreation());
        details.put("preferredDateTime", livraison.getDatePreferenceAgriculteur() != null ? livraison.getDatePreferenceAgriculteur() : livraison.getDateDepart());
        details.put("proposedDateTime", livraison.getDateProposeeNegociation());
        details.put("pricePerKm", round2(resolvePricePerKm(livraison)));
        details.put("pricingBreakdown", buildPricingBreakdown(livraison, prixSuggere));
        details.put("negociationRange", Map.of(
                "min", round2(prixSuggere * (1 - MAX_NEGOTIATION_DELTA)),
                "max", round2(prixSuggere * (1 + MAX_NEGOTIATION_DELTA))
        ));
        details.put("reference", safe(livraison.getReference()));
        return details;
    }

    private void initializeCreationDefaults(Livraison livraison) {
        LocalDateTime now = LocalDateTime.now();
        if (livraison.getReference() == null || livraison.getReference().isBlank())
            livraison.setReference("LIV-" + now.toLocalDate() + "-" + now.getNano());
        if (livraison.getDateCreation() == null) livraison.setDateCreation(now);
        if (livraison.getDateDemande() == null) livraison.setDateDemande(livraison.getDateCreation());
        if (livraison.getDateDepart() == null) livraison.setDateDepart(livraison.getDateCreation());
        if (livraison.getDatePreferenceAgriculteur() == null) livraison.setDatePreferenceAgriculteur(livraison.getDateDepart());
        if (livraison.getStatus() == null) livraison.setStatus(StatusLivraison.EN_ATTENTE);
        if (livraison.getStatusDemande() == null) livraison.setStatusDemande(StatusDemande.SOUMISE);
        if (livraison.getQuantiteProduit() <= 0) {
            livraison.setQuantiteProduit(livraison.getPoids() > 0 ? livraison.getPoids() : 1.0);
        }
        if (livraison.getUniteProduit() == null || livraison.getUniteProduit().isBlank()) {
            livraison.setUniteProduit("kg");
        }
        if (livraison.getLatActuelle() == 0.0 && livraison.getLngActuelle() == 0.0) {
            livraison.setLatActuelle(livraison.getLatDepart());
            livraison.setLngActuelle(livraison.getLngDepart());
        }
    }

    private void safeValidateAgriculteurOnCreate(int agriculteurId) {
        if (agriculteurId <= 0) {
            log.warn("Creation livraison sans agriculteurId valide (value={})", agriculteurId);
            return;
        }
        try {
            validateAgriculteur(agriculteurId);
        } catch (ResponseStatusException ex) {
            if (strictUserValidation) throw ex;
            log.warn("Validation agriculteur bypass en creation (userId={}): {}", agriculteurId, ex.getReason());
        } catch (Exception ex) {
            if (strictUserValidation) throw ex;
            log.warn("Validation agriculteur bypass en creation (userId={}): {}", agriculteurId, ex.getMessage());
        }
    }

    private void validateAgriculteur(int agriculteurId) {
        if (userClient == null) return;
        if (agriculteurId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "agriculteurId invalide");
        }
        UserSummaryDto user = fetchUser(agriculteurId);
        if (user == null) return;
        if (!"AGRICULTEUR".equalsIgnoreCase(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Utilisateur " + agriculteurId + " n'a pas le role AGRICULTEUR.");
        }
    }

    private void validateLivreur(int livreurId, boolean requireAvailable) {
        if (userClient == null) return;
        if (livreurId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "livreurId invalide");
        }
        UserSummaryDto user = fetchUser(livreurId);
        if (user == null) return;
        if (!"TRANSPORTEUR".equalsIgnoreCase(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Utilisateur " + livreurId + " n'a pas le role TRANSPORTEUR.");
        }
        if (user.statusCompte() != null && "REJETE".equalsIgnoreCase(user.statusCompte())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Compte livreur rejete, operation non autorisee.");
        }
        if (requireAvailable && Boolean.FALSE.equals(user.disponible())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Livreur indisponible pour cette operation.");
        }
    }

    private UserSummaryDto fetchUser(int userId) {
        try {
            UserSummaryDto user = userClient.getInternalUser(userId);
            if (user == null || user.id() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Utilisateur introuvable: " + userId);
            }
            return user;
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Utilisateur introuvable: " + userId);
        } catch (FeignException e) {
            if (!strictUserValidation) {
                log.warn("user-service indisponible, validation bypass pour userId={} (status={})", userId, e.status());
                return null;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Erreur de communication avec user-service.");
        } catch (Exception e) {
            if (!strictUserValidation) {
                log.warn("validation user bypass (exception inattendue) pour userId={}: {}", userId, e.getMessage());
                return null;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Erreur de communication avec user-service.");
        }
    }

    private double calculerDistance(Livraison livraison) {
        double R = 6371.0;
        double dLat = Math.toRadians(livraison.getLatArrivee() - livraison.getLatDepart());
        double dLng = Math.toRadians(livraison.getLngArrivee() - livraison.getLngDepart());
        double sLat = Math.toRadians(livraison.getLatDepart());
        double eLat = Math.toRadians(livraison.getLatArrivee());
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(sLat)*Math.cos(eLat)*Math.sin(dLng/2)*Math.sin(dLng/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    private double calculerPrixSuggere(Livraison livraison) {
        WeatherPricingService.WeatherSnapshot weather = fetchWeatherSnapshot(livraison.getLatDepart(), livraison.getLngDepart());
        return calculerPrixSuggere(livraison, weather);
    }

    private double calculerPrixSuggere(Livraison livraison, WeatherPricingService.WeatherSnapshot weather) {
        TypeLivraison type = livraison.getType() == null ? TypeLivraison.LOCALE : livraison.getType();
        boolean isLong = type == TypeLivraison.LONGUE_DISTANCE;
        double distance = Math.max(0, resolveDistanceKm(livraison));
        double poids = Math.max(0, livraison.getPoids());
        double base = isLong ? LONG_DISTANCE_BASE_FEE : LOCAL_BASE_FEE;
        double pricePerKm = resolvePricePerKm(livraison);
        double prixDistance = distance * pricePerKm;
        double prixQuantite = calculerSupplementQuantite(poids);
        double subtotal = base + prixDistance + prixQuantite;
        double weatherAmount = subtotal * Math.max(weather.surchargePercent(), 0.0);
        double groupingDiscount = livraison.isEstRegroupable() ? subtotal * 0.08 : 0.0;
        double total = subtotal + weatherAmount - groupingDiscount;
        return Math.max(12.0, round2(total));
    }

    private double resolvePricePerKm(Livraison livraison) {
        TypeLivraison type = livraison.getType() == null ? TypeLivraison.LOCALE : livraison.getType();
        return type == TypeLivraison.LONGUE_DISTANCE ? LONG_DISTANCE_PRICE_PER_KM : LOCAL_PRICE_PER_KM;
    }

    private double calculerSupplementQuantite(double poidsKg) {
        double poids = Math.max(poidsKg, 0);
        if (poids == 0) {
            return 0.0;
        }

        double supplement = 0.0;
        supplement += trancheQuantite(poids, 0, 100, 0.060);
        supplement += trancheQuantite(poids, 100, 500, 0.045);
        supplement += trancheQuantite(poids, 500, 2000, 0.030);
        supplement += trancheQuantite(poids, 2000, Double.MAX_VALUE, 0.020);
        return round2(supplement);
    }

    private double trancheQuantite(double poids, double min, double max, double prixParKg) {
        if (poids <= min) {
            return 0.0;
        }
        double upperBound = Math.min(poids, max);
        return Math.max(upperBound - min, 0.0) * prixParKg;
    }

    private Map<String, Object> buildPricingBreakdown(Livraison livraison, double prixSuggere) {
        WeatherPricingService.WeatherSnapshot weather = fetchWeatherSnapshot(livraison.getLatDepart(), livraison.getLngDepart());
        double distance = round2(resolveDistanceKm(livraison));
        double pricePerKm = round2(resolvePricePerKm(livraison));
        double distanceAmount = round2(distance * pricePerKm);
        double quantityAmount = calculerSupplementQuantite(livraison.getPoids());
        double baseFee = resolvePricePerKm(livraison) == LONG_DISTANCE_PRICE_PER_KM ? LONG_DISTANCE_BASE_FEE : LOCAL_BASE_FEE;
        double weatherAmount = round2((baseFee + distanceAmount + quantityAmount) * Math.max(weather.surchargePercent(), 0.0));
        double discount = livraison.isEstRegroupable() ? round2((baseFee + distanceAmount + quantityAmount) * 0.08) : 0.0;

        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("distanceKm", distance);
        breakdown.put("pricePerKm", pricePerKm);
        breakdown.put("distanceAmount", distanceAmount);
        breakdown.put("quantityKg", round2(Math.max(livraison.getPoids(), 0)));
        breakdown.put("quantityAmount", quantityAmount);
        breakdown.put("baseFee", round2(baseFee));
        breakdown.put("weatherSurchargeAmount", weatherAmount);
        breakdown.put("weatherSurchargePercent", round2(Math.max(weather.surchargePercent(), 0.0) * 100.0));
        breakdown.put("weatherCondition", weather.condition());
        breakdown.put("groupingDiscount", discount);
        breakdown.put("total", prixSuggere);
        breakdown.put("quantityPricingRule", "Tarif/kg dégressif selon la quantité");
        return breakdown;
    }

    private int calculerRetard(Livraison livraison) {
        if (livraison.getDateLivraisonPrevue() == null || livraison.getDateLivraisonEffective() == null) return 0;
        long retard = Duration.between(livraison.getDateLivraisonPrevue(), livraison.getDateLivraisonEffective()).toMinutes();
        return (int) Math.max(retard, 0);
    }

    private void assertTransporterDayCapacity(Livraison livraison, int transporteurId) {
        if (transporteurId <= 0) {
            return;
        }

        LocalDateTime plannedDateTime = resolveTransporterCalendarDate(livraison);
        if (plannedDateTime == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cette commande doit avoir une date et une heure prevues avant d'etre affectee au transporteur."
            );
        }

        List<Livraison> sameDayDeliveries = livraisonRepository.findByTransporteurIdOrderByDateCreationDesc(transporteurId).stream()
                .filter(existing -> existing.getId() == null || !existing.getId().equals(livraison.getId()))
                .filter(existing -> existing.getStatus() != StatusLivraison.ANNULEE)
                .filter(existing -> {
                    LocalDateTime existingDate = resolveTransporterCalendarDate(existing);
                    return existingDate != null && existingDate.toLocalDate().equals(plannedDateTime.toLocalDate());
                })
                .toList();

        List<Livraison> deliveriesToCheck = new ArrayList<>(sameDayDeliveries);
        deliveriesToCheck.add(cloneForCapacityCheck(livraison, transporteurId));

        DayCapacitySnapshot snapshot = analyzeDayCapacity(deliveriesToCheck);
        if (snapshot.hasConflict()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    snapshot.warningMessage() != null
                            ? snapshot.warningMessage()
                            : "Le planning de ce transporteur est deja surcharge pour cette journee."
            );
        }
    }

    private Livraison cloneForCapacityCheck(Livraison source, int transporteurId) {
        Livraison copy = new Livraison();
        copy.setId(source.getId());
        copy.setReference(source.getReference());
        copy.setStatus(source.getStatus() != null ? source.getStatus() : StatusLivraison.ACCEPTEE);
        copy.setAgriculteurId(source.getAgriculteurId());
        copy.setTransporteurId(transporteurId);
        copy.setDateCreation(source.getDateCreation());
        copy.setDateDepart(source.getDateDepart());
        copy.setDateLivraisonPrevue(source.getDateLivraisonPrevue());
        copy.setAdresseDepart(source.getAdresseDepart());
        copy.setAdresseArrivee(source.getAdresseArrivee());
        copy.setLatDepart(source.getLatDepart());
        copy.setLngDepart(source.getLngDepart());
        copy.setLatArrivee(source.getLatArrivee());
        copy.setLngArrivee(source.getLngArrivee());
        copy.setDistanceKm(source.getDistanceKm());
        copy.setPoids(source.getPoids());
        copy.setVolume(source.getVolume());
        copy.setTypeProduit(source.getTypeProduit());
        copy.setPrix(source.getPrix());
        return copy;
    }

    private DayCapacitySnapshot analyzeDayCapacity(List<Livraison> deliveries) {
        if (deliveries == null || deliveries.isEmpty()) {
            return new DayCapacitySnapshot(List.of(), 0, 0, 0, MAX_TRANSPORTER_DAY_MINUTES, MAX_TRANSPORTER_DAY_MINUTES, 0, false, null, 0, null);
        }

        List<Livraison> sorted = deliveries.stream()
                .filter(l -> resolveTransporterCalendarDate(l) != null)
                .sorted(Comparator.comparing(this::resolveTransporterCalendarDate))
                .toList();

        List<Map<String, Object>> items = new ArrayList<>();
        Livraison previous = null;
        LocalDateTime rollingEnd = null;
        long totalServiceMinutes = 0;
        long totalTransitionMinutes = 0;
        long overlapCount = 0;

        for (Livraison delivery : sorted) {
            LocalDateTime scheduledStart = resolveTransporterCalendarDate(delivery);
            long pickupMinutes = estimatePickupMinutes(delivery);
            long driveMinutes = estimateDrivingMinutes(delivery);
            long dropoffMinutes = estimateDropoffMinutes(delivery);
            long serviceMinutes = pickupMinutes + driveMinutes + dropoffMinutes;
            long transitionMinutes = previous == null ? 0 : estimateTransitionMinutes(previous, delivery);

            LocalDateTime earliestStart = previous == null || rollingEnd == null
                    ? scheduledStart
                    : rollingEnd.plusMinutes(transitionMinutes);
            LocalDateTime estimatedStart = earliestStart.isAfter(scheduledStart) ? earliestStart : scheduledStart;
            long overlapMinutes = Math.max(0, Duration.between(scheduledStart, estimatedStart).toMinutes());
            if (overlapMinutes > 0) {
                overlapCount++;
            }
            LocalDateTime estimatedEnd = estimatedStart.plusMinutes(serviceMinutes);

            totalServiceMinutes += serviceMinutes;
            totalTransitionMinutes += transitionMinutes;

            Map<String, Object> itemData = new HashMap<>();
            StatusLivraison status = delivery.getStatus() != null ? delivery.getStatus() : StatusLivraison.EN_ATTENTE;
            itemData.put("livraisonId", delivery.getId());
            itemData.put("reference", safe(delivery.getReference()));
            itemData.put("status", status.name());
            itemData.put("icon", iconForStatus(status));
            itemData.put("color", colorForStatus(status));
            itemData.put("priority", priorityForStatus(status));
            itemData.put("adresseDepart", safe(delivery.getAdresseDepart()));
            itemData.put("adresseArrivee", safe(delivery.getAdresseArrivee()));
            itemData.put("prix", round2(delivery.getPrix()));
            itemData.put("typeProduit", safe(delivery.getTypeProduit()));
            itemData.put("scheduledDateTime", scheduledStart);
            itemData.put("estimatedStartTime", estimatedStart.toLocalTime().toString());
            itemData.put("estimatedEndTime", estimatedEnd.toLocalTime().toString());
            itemData.put("estimatedPickupMinutes", pickupMinutes);
            itemData.put("estimatedDriveMinutes", driveMinutes);
            itemData.put("estimatedDropoffMinutes", dropoffMinutes);
            itemData.put("transitionFromPreviousMinutes", transitionMinutes);
            itemData.put("overlapMinutes", overlapMinutes);
            itemData.put("estimatedTotalMinutes", serviceMinutes + transitionMinutes);
            itemData.put("distanceKm", round2(resolveDistanceKm(delivery)));
            itemData.put("grouped", delivery.isGrouped());
            itemData.put("groupReference", safe(delivery.getGroupReference()));
            items.add(itemData);

            previous = delivery;
            rollingEnd = estimatedEnd;
        }

        long totalEstimatedMinutes = totalServiceMinutes + totalTransitionMinutes;
        long remainingMinutes = Math.max(MAX_TRANSPORTER_DAY_MINUTES - totalEstimatedMinutes, 0);
        long overloadMinutes = Math.max(totalEstimatedMinutes - MAX_TRANSPORTER_DAY_MINUTES, 0);
        boolean spillsToNextDay = rollingEnd != null && !rollingEnd.toLocalDate().equals(resolveTransporterCalendarDate(sorted.get(0)).toLocalDate());
        boolean hasConflict = overloadMinutes > 0 || overlapCount > 0 || spillsToNextDay;
        String projectedEndTime = rollingEnd != null ? rollingEnd.toLocalTime().toString() : null;
        String warningMessage = buildCapacityWarningMessage(sorted.get(0), totalEstimatedMinutes, overloadMinutes, overlapCount, spillsToNextDay, rollingEnd);

        return new DayCapacitySnapshot(
                items,
                totalEstimatedMinutes,
                totalServiceMinutes,
                totalTransitionMinutes,
                MAX_TRANSPORTER_DAY_MINUTES,
                remainingMinutes,
                overloadMinutes,
                hasConflict,
                projectedEndTime,
                overlapCount,
                warningMessage
        );
    }

    private String buildCapacityWarningMessage(
            Livraison sample,
            long totalEstimatedMinutes,
            long overloadMinutes,
            long overlapCount,
            boolean spillsToNextDay,
            LocalDateTime projectedEnd) {
        LocalDateTime referenceDate = resolveTransporterCalendarDate(sample);
        String dateLabel = referenceDate != null ? referenceDate.toLocalDate().toString() : "ce jour";
        if (overloadMinutes > 0) {
            return "Planning impossible le " + dateLabel
                    + " : charge estimee " + formatMinutes(totalEstimatedMinutes)
                    + " pour un maximum de " + formatMinutes(MAX_TRANSPORTER_DAY_MINUTES)
                    + ". Depassement estime : " + formatMinutes(overloadMinutes) + ".";
        }
        if (spillsToNextDay) {
            return "Planning impossible le " + dateLabel
                    + " : la derniere livraison deborderait sur le jour suivant.";
        }
        if (overlapCount > 0) {
            return "Planning conflictuel le " + dateLabel
                    + " : certaines livraisons se chevauchent apres ajout des temps de trajet et de livraison.";
        }
        return null;
    }

    private long estimatePickupMinutes(Livraison livraison) {
        long byWeight = Math.round(Math.max(livraison.getPoids(), 0.0) / 25.0) * 5;
        long byVolume = Math.round(Math.max(livraison.getVolume(), 0.0) * 10.0);
        return Math.min(MAX_PICKUP_MINUTES, Math.max(MIN_PICKUP_MINUTES, MIN_PICKUP_MINUTES + byWeight + byVolume));
    }

    private long estimateDropoffMinutes(Livraison livraison) {
        long byWeight = Math.round(Math.max(livraison.getPoids(), 0.0) / 30.0) * 4;
        long byVolume = Math.round(Math.max(livraison.getVolume(), 0.0) * 8.0);
        return Math.min(MAX_DROPOFF_MINUTES, Math.max(MIN_DROPOFF_MINUTES, MIN_DROPOFF_MINUTES + byWeight + byVolume));
    }

    private long estimateDrivingMinutes(Livraison livraison) {
        double distance = resolveDistanceKm(livraison);
        if (distance <= 0) {
            return 30;
        }
        return Math.max(15, Math.round((distance / AVERAGE_DRIVING_SPEED_KMH) * 60.0));
    }

    private long estimateTransitionMinutes(Livraison previous, Livraison current) {
        double distance = haversineKm(
                previous.getLatArrivee(),
                previous.getLngArrivee(),
                current.getLatDepart(),
                current.getLngDepart()
        );
        if (distance <= 0.2) {
            return SAME_SITE_BUFFER_MINUTES;
        }
        return Math.max(SAME_SITE_BUFFER_MINUTES, Math.round((distance / AVERAGE_DRIVING_SPEED_KMH) * 60.0));
    }

    private double resolveDistanceKm(Livraison livraison) {
        if (livraison.getDistanceKm() > 0) {
            return livraison.getDistanceKm();
        }
        return haversineKm(
                livraison.getLatDepart(),
                livraison.getLngDepart(),
                livraison.getLatArrivee(),
                livraison.getLngArrivee()
        );
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        if (!Double.isFinite(lat1) || !Double.isFinite(lng1) || !Double.isFinite(lat2) || !Double.isFinite(lng2)) {
            return 0.0;
        }

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double originLat = Math.toRadians(lat1);
        double destinationLat = Math.toRadians(lat2);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(originLat) * Math.cos(destinationLat)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }

    private WeatherPricingService.WeatherSnapshot fetchWeatherSnapshot(double lat, double lng) {
        if (weatherPricingService == null || !isValidCoordinate(lat, lng)) {
            return new WeatherPricingService.WeatherSnapshot(0.0, 0.0, "unknown", 0.0, LocalDateTime.now(), true);
        }
        return weatherPricingService.fetchCurrentWeather(lat, lng);
    }

    private WeatherSnapshotDto toWeatherSnapshotDto(WeatherPricingService.WeatherSnapshot weather) {
        return new WeatherSnapshotDto(
                round2(Math.max(weather.windSpeedKmh(), 0.0)),
                round2(Math.max(weather.precipitationMm(), 0.0)),
                weather.condition(),
                round2(Math.max(weather.surchargePercent(), 0.0) * 100.0),
                weather.fetchedAt(),
                weather.fallback()
        );
    }

    private String formatMinutes(long minutes) {
        long safeMinutes = Math.max(minutes, 0);
        long hours = safeMinutes / 60;
        long remainingMinutes = safeMinutes % 60;
        return hours + "h" + String.format("%02d", remainingMinutes);
    }

    private List<Map<String, Object>> buildGroupRoutePoints(List<Livraison> deliveries) {
        List<Livraison> validDeliveries = deliveries.stream()
                .filter(delivery -> isValidCoordinate(delivery.getLatDepart(), delivery.getLngDepart()))
                .filter(delivery -> isValidCoordinate(delivery.getLatArrivee(), delivery.getLngArrivee()))
                .toList();

        if (validDeliveries.isEmpty()) {
            return List.of();
        }

        Livraison originDelivery = validDeliveries.stream()
                .sorted(Comparator.comparing(this::resolveTransporterCalendarDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .orElse(validDeliveries.get(0));

        List<Livraison> remaining = new ArrayList<>(validDeliveries);
        List<Map<String, Object>> routePoints = new ArrayList<>();

        routePoints.add(Map.of(
                "lat", originDelivery.getLatDepart(),
                "lng", originDelivery.getLngDepart(),
                "label", safe(originDelivery.getAdresseDepart()).isBlank()
                        ? safe(originDelivery.getReference()) + " - depart"
                        : safe(originDelivery.getAdresseDepart()),
                "kind", "start"
        ));

        double currentLat = originDelivery.getLatDepart();
        double currentLng = originDelivery.getLngDepart();

        while (!remaining.isEmpty()) {
            Livraison next = remaining.get(0);
            double nearestDistance = haversineKm(
                    currentLat,
                    currentLng,
                    next.getLatArrivee(),
                    next.getLngArrivee()
            );

            for (Livraison candidate : remaining) {
                double candidateDistance = haversineKm(
                        currentLat,
                        currentLng,
                        candidate.getLatArrivee(),
                        candidate.getLngArrivee()
                );
                if (candidateDistance < nearestDistance) {
                    next = candidate;
                    nearestDistance = candidateDistance;
                }
            }

            routePoints.add(Map.of(
                    "lat", next.getLatArrivee(),
                    "lng", next.getLngArrivee(),
                    "label", safe(next.getReference()) + " - arrivee",
                    "kind", "stop"
            ));

            currentLat = next.getLatArrivee();
            currentLng = next.getLngArrivee();
            remaining.remove(next);
        }

        return routePoints;
    }

    private List<Livraison> resolveGroupDeliveries(int transporteurId, List<Integer> livraisonIds, String currentGroupReference) {
        if (livraisonIds == null || livraisonIds.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins 2 livraisons sont requises pour gérer un groupe");
        }

        List<Livraison> livraisons = livraisonIds.stream()
                .distinct()
                .map(this::findById)
                .toList();

        for (Livraison livraison : livraisons) {
            if (livraison.getTransporteurId() != transporteurId) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La livraison " + livraison.getId() + " n'est pas assignée à ce transporteur");
            }
            if (livraison.isGrouped()) {
                String existingGroupReference = safe(livraison.getGroupReference());
                boolean sameGroup = !existingGroupReference.isBlank() && existingGroupReference.equals(currentGroupReference);
                if (!sameGroup) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La livraison " + livraison.getId() + " est déjà dans un autre groupe");
                }
            }
        }

        return livraisons;
    }

    private double applyGrouping(List<Livraison> livraisons, String groupReference, LocalDateTime groupedAt) {
        double totalSavings = 0.0;
        for (Livraison livraison : livraisons) {
            double originalPrice = livraison.getPrixAvantRegroupement() != null && livraison.getPrixAvantRegroupement() > 0
                    ? livraison.getPrixAvantRegroupement()
                    : livraison.getPrix();
            double discountedPrice = round2(originalPrice * 0.75);
            totalSavings += Math.max(originalPrice - discountedPrice, 0);

            livraison.setGrouped(true);
            livraison.setGroupReference(groupReference);
            livraison.setGroupedAt(groupedAt);
            if (livraison.getPrixAvantRegroupement() == null || livraison.getPrixAvantRegroupement() <= 0) {
                livraison.setPrixAvantRegroupement(originalPrice);
            }
            livraison.setPrix(discountedPrice);
        }
        return round2(totalSavings);
    }

    private void resetGrouping(List<Livraison> livraisons) {
        for (Livraison livraison : livraisons) {
            livraison.setGrouped(false);
            livraison.setGroupReference(null);
            livraison.setGroupedAt(null);
            if (livraison.getPrixAvantRegroupement() != null && livraison.getPrixAvantRegroupement() > 0) {
                livraison.setPrix(round2(livraison.getPrixAvantRegroupement()));
            }
            livraison.setPrixAvantRegroupement(null);
        }
    }

    private Map<String, Object> buildGroupMutationResponse(
            String groupReference,
            List<Livraison> deliveries,
            double totalSavings,
            String message) {
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("groupReference", groupReference);
        resultData.put("deliveries", deliveries);
        resultData.put("deliveriesCount", deliveries.size());
        resultData.put("totalSavings", round2(totalSavings));
        resultData.put("savingsPercentage", 25.0);
        resultData.put("message", message);
        return resultData;
    }

    private boolean isValidCoordinate(double lat, double lng) {
        return Double.isFinite(lat)
                && Double.isFinite(lng)
                && (lat != 0.0 || lng != 0.0);
    }

    private record DayCapacitySnapshot(
            List<Map<String, Object>> items,
            long totalEstimatedMinutes,
            long totalServiceMinutes,
            long totalTransitionMinutes,
            long capacityMinutes,
            long remainingMinutes,
            long overloadMinutes,
            boolean hasConflict,
            String projectedEndTime,
            long overlapCount,
            String warningMessage
    ) {
    }

    private String safe(String s) { return s != null ? s : ""; }
    private double round2(double value) { return Math.round(value * 100.0) / 100.0; }

    private String iconForStatus(StatusLivraison status) {
        if (status == null) return "help";
        return switch (status) {
            case EN_ATTENTE -> "pending";
            case ACCEPTEE -> "check_circle";
            case EN_COURS -> "local_shipping";
            case RETARD -> "warning";
            case LIVREE -> "done_all";
            case ANNULEE -> "cancel";
        };
    }
    
    private String colorForStatus(StatusLivraison status) {
        if (status == null) return "#9E9E9E";
        return switch (status) {
            case EN_ATTENTE -> "#FF9800";
            case ACCEPTEE -> "#4CAF50";
            case EN_COURS -> "#2196F3";
            case RETARD -> "#F44336";
            case LIVREE -> "#8BC34A";
            case ANNULEE -> "#9E9E9E";
        };
    }
    
    private int priorityForStatus(StatusLivraison status) {
        if (status == null) return 0;
        return switch (status) {
            case EN_COURS -> 5;
            case RETARD -> 4;
            case ACCEPTEE -> 3;
            case EN_ATTENTE -> 2;
            case LIVREE -> 1;
            case ANNULEE -> 0;
        };
    }

    // Méthodes manquantes pour la compatibilité avec le contrôleur
    @Transactional(readOnly = true)
    public Map<String, Object> buildAdminKpis() {
        List<Livraison> all = getAllLivraison();
        
        long total = all.size();
        long enAttente = all.stream().filter(l -> l.getStatus() == StatusLivraison.EN_ATTENTE).count();
        long acceptees = all.stream().filter(l -> l.getStatus() == StatusLivraison.ACCEPTEE).count();
        long enCours = all.stream().filter(l -> l.getStatus() == StatusLivraison.EN_COURS).count();
        long livre = all.stream().filter(l -> l.getStatus() == StatusLivraison.LIVREE).count();
        long annulees = all.stream().filter(l -> l.getStatus() == StatusLivraison.ANNULEE).count();
        
        double revenue = all.stream().filter(l -> l.getStatus() == StatusLivraison.LIVREE).mapToDouble(Livraison::getPrix).sum();
        double avgPrice = total > 0 ? all.stream().mapToDouble(Livraison::getPrix).average().orElse(0.0) : 0.0;
        
        Map<String, Object> kpis = new HashMap<>();
        kpis.put("total", total);
        kpis.put("enAttente", enAttente);
        kpis.put("acceptees", acceptees);
        kpis.put("enCours", enCours);
        kpis.put("livre", livre);
        kpis.put("annulees", annulees);
        kpis.put("revenue", round2(revenue));
        kpis.put("avgPrice", round2(avgPrice));
        kpis.put("tauxLivraison", total > 0 ? round2((livre * 100.0) / total) : 0.0);
        
        return kpis;
    }

    public Livraison updateLivraison(Integer id, Livraison livraison) {
        Livraison existing = findById(id);
        assertPendingForCustomerChanges(existing);

        if (isProvidedText(livraison.getReference())) {
            existing.setReference(livraison.getReference());
        }
        if (isProvidedText(livraison.getAdresseDepart())) {
            existing.setAdresseDepart(livraison.getAdresseDepart());
        }
        if (isProvidedText(livraison.getAdresseArrivee())) {
            existing.setAdresseArrivee(livraison.getAdresseArrivee());
        }
        if (isValidCoordinate(livraison.getLatDepart(), livraison.getLngDepart())) {
            existing.setLatDepart(livraison.getLatDepart());
            existing.setLngDepart(livraison.getLngDepart());
        }
        if (isValidCoordinate(livraison.getLatArrivee(), livraison.getLngArrivee())) {
            existing.setLatArrivee(livraison.getLatArrivee());
            existing.setLngArrivee(livraison.getLngArrivee());
        }
        if (livraison.getPoids() > 0) {
            existing.setPoids(livraison.getPoids());
        }
        if (livraison.getVolume() > 0) {
            existing.setVolume(livraison.getVolume());
        }
        if (isProvidedText(livraison.getTypeProduit())) {
            existing.setTypeProduit(livraison.getTypeProduit());
        }
        if (livraison.getDetailsDemande() != null) {
            existing.setDetailsDemande(livraison.getDetailsDemande());
        }

        existing.setEstRegroupable(livraison.isEstRegroupable());

        if (livraison.getDateLivraisonPrevue() != null) {
            existing.setDateLivraisonPrevue(livraison.getDateLivraisonPrevue());
        }

        // Backward compatibility front: certains ecrans envoient la date de depart dans dateCreation.
        LocalDateTime requestedDeparture = livraison.getDateDepart() != null
                ? livraison.getDateDepart()
                : livraison.getDateCreation();
        if (requestedDeparture != null) {
            existing.setDateDepart(requestedDeparture);
        }
        if (livraison.getDatePreferenceAgriculteur() != null) {
            existing.setDatePreferenceAgriculteur(livraison.getDatePreferenceAgriculteur());
        } else if (requestedDeparture != null) {
            existing.setDatePreferenceAgriculteur(requestedDeparture);
        }

        if (livraison.getStatus() != null) {
            existing.setStatus(livraison.getStatus());
        }
        if (livraison.getType() != null) {
            existing.setType(livraison.getType());
        }
        if (livraison.getAgriculteurId() > 0) {
            existing.setAgriculteurId(livraison.getAgriculteurId());
        }
        if (livraison.getTransporteurId() > 0) {
            existing.setTransporteurId(livraison.getTransporteurId());
        }

        applyBusinessRules(existing);
        if (livraison.getPrix() > 0) {
            existing.setPrix(round2(livraison.getPrix()));
        }
        return livraisonRepository.save(existing);
    }

    public void delete(Integer id) {
        Livraison existing = findById(id);
        assertPendingForCustomerChanges(existing);
        livraisonRepository.deleteById(existing.getId());
    }

    public Livraison updateStatus(Integer id, StatusLivraison status) {
        Livraison livraison = findById(id);
        livraison.setStatus(status);
        if (status == StatusLivraison.EN_COURS) {
            livraison.setStatusDemande(StatusDemande.EN_TRAITEMENT);
            prepareNotification(
                    livraison,
                    livraison.getTransporteurId(),
                    livraison.getAgriculteurId(),
                    "DELIVERY_IN_TRANSIT",
                    "Livreur en route",
                    "Le livreur est en route pour collecter la commande " + safe(livraison.getReference()) + ".",
                    "PENDING");
        }
        if (status == StatusLivraison.LIVREE && livraison.getDateLivraisonEffective() == null) {
            livraison.setDateLivraisonEffective(LocalDateTime.now());
            if (livraison.getNote() <= 0 && !"IGNORED".equalsIgnoreCase(safe(livraison.getRatingStatus()))) {
                livraison.setRatingStatus("PENDING");
                livraison.setRatingDecisionAt(null);
            }
            livraison.setSignatureStatus("PENDING_SIGNATURE");
            prepareNotification(
                    livraison,
                    livraison.getTransporteurId(),
                    livraison.getAgriculteurId(),
                    "DELIVERY_SIGNATURE_REQUIRED",
                    "Signature requise",
                    "Votre livraison " + safe(livraison.getReference()) + " est arrivée. Veuillez signer pour confirmer la réception.",
                    "PENDING");
        }
        return livraisonRepository.save(livraison);
    }

    public Livraison saveSignature(Integer id, String signatureData, int agriculteurId) {
        Livraison livraison = findById(id);
        if (livraison.getStatus() != StatusLivraison.LIVREE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La livraison n'est pas encore marquée comme livrée.");
        }
        if (livraison.getAgriculteurId() != agriculteurId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul l'agriculteur concerné peut signer.");
        }
        if ("SIGNED".equalsIgnoreCase(safe(livraison.getSignatureStatus()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette livraison a déjà été signée.");
        }
        livraison.setSignatureData(signatureData);
        livraison.setSignedAt(LocalDateTime.now());
        livraison.setSignatureStatus("SIGNED");
        livraison.setNotificationStatus("ACCEPTED");
        livraison.setNotificationHandledAt(LocalDateTime.now());
        livraison.setNotificationSeen(true);
        return livraisonRepository.save(livraison);
    }

    public Livraison startRouteToPickup(Integer id, int transporteurId) {
        validateLivreur(transporteurId, false);
        Livraison livraison = findById(id);

        if (livraison.getStatus() == StatusLivraison.LIVREE || livraison.getStatus() == StatusLivraison.ANNULEE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette livraison n'est plus active.");
        }

        if (livraison.getTransporteurId() > 0 && livraison.getTransporteurId() != transporteurId) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette livraison est deja prise par un autre transporteur.");
        }

        if (livraison.getTransporteurId() <= 0) {
            assertTransporterDayCapacity(livraison, transporteurId);
            livraison.setTransporteurId(transporteurId);
        }

        livraison.setStatus(StatusLivraison.EN_COURS);
        livraison.setStatusDemande(StatusDemande.EN_TRAITEMENT);
        if (livraison.getDateDepart() == null) {
            livraison.setDateDepart(LocalDateTime.now());
        }

        prepareNotification(
                livraison,
                transporteurId,
                livraison.getAgriculteurId(),
                "DELIVERY_IN_TRANSIT",
                "Livreur en route",
                "Le livreur est en route vers votre point de collecte pour la livraison " + safe(livraison.getReference()) + ".",
                "PENDING");

        return livraisonRepository.save(livraison);
    }

    public Livraison assignTransporteur(Integer id, int transporteurId) {
        validateLivreur(transporteurId, true);
        Livraison livraison = findById(id);
        if (isAlreadyAssignedToTransporteur(livraison, transporteurId)) {
            return livraison;
        }
        assertAssignableToTransporteur(livraison, transporteurId);
        assertTransporterDayCapacity(livraison, transporteurId);
        livraison.setTransporteurId(transporteurId);
        livraison.setStatus(StatusLivraison.ACCEPTEE);
        livraison.setStatusDemande(StatusDemande.EN_TRAITEMENT);
        prepareNotification(
                livraison,
                transporteurId,
                livraison.getAgriculteurId(),
                "DELIVERY_ASSIGNED",
                "Livraison acceptée",
                "Un livreur a accepté la livraison " + safe(livraison.getReference()) + ".",
                "PENDING");
        return livraisonRepository.save(livraison);
    }

    private boolean isAlreadyAssignedToTransporteur(Livraison livraison, int transporteurId) {
        return livraison.getTransporteurId() == transporteurId
                && (livraison.getStatus() == StatusLivraison.ACCEPTEE
                || livraison.getStatus() == StatusLivraison.EN_COURS
                || livraison.getStatus() == StatusLivraison.RETARD);
    }

    private void assertAssignableToTransporteur(Livraison livraison, int transporteurId) {
        StatusLivraison currentStatus = livraison.getStatus();
        if (currentStatus == StatusLivraison.LIVREE || currentStatus == StatusLivraison.ANNULEE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette livraison n'est plus disponible pour acceptation."
            );
        }

        if (livraison.getTransporteurId() > 0 && livraison.getTransporteurId() != transporteurId) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette livraison a deja ete acceptee par un autre livreur."
            );
        }

        if (currentStatus != null
                && currentStatus != StatusLivraison.EN_ATTENTE
                && !isAlreadyAssignedToTransporteur(livraison, transporteurId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette livraison n'est plus en attente d'acceptation."
            );
        }
    }

    public Livraison updateCurrentPosition(Integer id, double lat, double lng) {
        Livraison livraison = findById(id);
        livraison.setLatActuelle(lat);
        livraison.setLngActuelle(lng);
        return livraisonRepository.save(livraison);
    }

    public Livraison evaluerTransporteur(Integer id, double note, Integer evaluateurId) {
        if (note < 1 || note > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La note doit être comprise entre 1 et 5.");
        }
        Livraison livraison = findById(id);
        if (livraison.getStatus() != StatusLivraison.LIVREE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La notation est possible uniquement pour une livraison livree.");
        }

        if (evaluateurId == null || evaluateurId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "evaluateurId invalide");
        }

        if (livraison.getAgriculteurId() != evaluateurId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul l'agriculteur proprietaire peut noter cette livraison.");
        }

        if ("IGNORED".equalsIgnoreCase(safe(livraison.getRatingStatus()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La notation a ete ignoree pour cette livraison.");
        }

        livraison.setNote(round2(note));
        livraison.setRatingStatus("RATED");
        livraison.setRatingDecisionAt(LocalDateTime.now());
        return livraisonRepository.save(livraison);
    }

    public Livraison ignorerNotationTransporteur(Integer id, int agriculteurId) {
        if (agriculteurId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "agriculteurId invalide");
        }

        Livraison livraison = findById(id);
        if (livraison.getStatus() != StatusLivraison.LIVREE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'option ignorer est disponible uniquement pour une livraison livree.");
        }
        if (livraison.getAgriculteurId() != agriculteurId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul l'agriculteur proprietaire peut ignorer la notation.");
        }
        if (livraison.getNote() > 0 || "RATED".equalsIgnoreCase(safe(livraison.getRatingStatus()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette livraison est deja notee.");
        }

        livraison.setRatingStatus("IGNORED");
        livraison.setRatingDecisionAt(LocalDateTime.now());
        return livraisonRepository.save(livraison);
    }

    private void assertPendingForCustomerChanges(Livraison livraison) {
        if (livraison.getStatus() != StatusLivraison.EN_ATTENTE || livraison.getTransporteurId() > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La demande ne peut plus être modifiée ou supprimée après acceptation par un livreur."
            );
        }
    }

    private boolean isProvidedText(String value) {
        return value != null && !value.isBlank();
    }

    // Notifications intégrées au module delivery
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNotificationsForUser(int userId) {
        return livraisonRepository.findByNotificationToUserIdAndNotificationStatusNotNullOrderByNotificationCreatedAtDesc(userId)
                .stream()
                .map(this::toNotificationPayload)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingNegotiationNotifications(int userId) {
        return livraisonRepository.findByNotificationToUserIdAndNotificationStatusNotNullOrderByNotificationCreatedAtDesc(userId)
                .stream()
                .filter(livraison -> "PRICE_NEGOTIATION_BAR".equalsIgnoreCase(livraison.getNotificationType()))
                .filter(livraison -> {
                    String notificationStatus = safe(livraison.getNotificationStatus()).toUpperCase();
                    return "PENDING".equals(notificationStatus) || "COUNTERED".equals(notificationStatus);
                })
                .map(this::toNotificationPayload)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getUnreadNotificationsCount(int userId) {
        long unreadCount = livraisonRepository.countByNotificationToUserIdAndNotificationSeenFalse(userId);
        Map<String, Object> countData = new HashMap<>();
        countData.put("count", unreadCount);
        countData.put("userId", userId);
        countData.put("hasUnread", unreadCount > 0);
        return countData;
    }
    
    @Transactional
    public Map<String, Object> markAllNotificationsAsRead(int userId) {
        List<Livraison> notifications = livraisonRepository
                .findByNotificationToUserIdAndNotificationStatusNotNullOrderByNotificationCreatedAtDesc(userId);
        notifications.forEach(notification -> notification.setNotificationSeen(true));
        livraisonRepository.saveAll(notifications);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Notifications marquées comme lues");
        result.put("userId", userId);
        result.put("markedCount", notifications.size());
        result.put("totalNotifications", notifications.size());
        return result;
    }

    @Transactional
    public Map<String, Object> deleteNotification(int livraisonId, int actorId) {
        Livraison livraison = findById(livraisonId);
        if (actorId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "actorId invalide");
        }
        if (livraison.getNotificationToUserId() == null || livraison.getNotificationToUserId() != actorId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cette notification n'appartient pas a cet utilisateur.");
        }

        clearNotificationFields(livraison);
        livraisonRepository.save(livraison);

        Map<String, Object> result = new HashMap<>();
        result.put("id", livraisonId);
        result.put("deleted", true);
        result.put("actorId", actorId);
        result.put("message", "Notification supprimée");
        return result;
    }

    @Transactional
    public Map<String, Object> handleNotificationAction(int livraisonId, int actorId, String action, Double counterPrice, LocalDateTime counterDateTime) {
        Livraison livraison = findById(livraisonId);
        String normalizedAction = safe(action).trim().toUpperCase();
        Integer currentSender = livraison.getNotificationFromUserId();
        Integer replyTarget = resolveNotificationTarget(livraison, actorId, currentSender);

        switch (normalizedAction) {
            case "ACCEPT":
                Double prixFinal = resolveAcceptedNegotiationPrice(livraison);
                if (prixFinal != null && prixFinal > 0) {
                    LocalDateTime plannedDate = resolveAcceptedNegotiationDate(livraison);
                    if (plannedDate != null) {
                        livraison.setDateLivraisonPrevue(plannedDate);
                    }
                    try {
                        assertTransporterDayCapacity(livraison, livraison.getTransporteurId());
                    } catch (ResponseStatusException ex) {
                        log.warn("Acceptation notification: controle capacite ignore pour livraison id={} (reason={})",
                                livraison.getId(), ex.getReason());
                    }
                    livraison.setPrix(prixFinal);
                    livraison.setStatus(StatusLivraison.EN_COURS);
                    livraison.setStatusDemande(StatusDemande.EN_TRAITEMENT);
                    livraison.setStatusNegociation("ACCEPTEE_NEGO");
                }
                prepareNotification(
                        livraison,
                        actorId,
                        replyTarget,
                        "PRICE_NEGOTIATION_BAR",
                        "Proposition acceptée",
                        "Votre proposition a été acceptée pour la livraison " + safe(livraison.getReference()),
                        "ACCEPTED");
                break;
            case "REJECT":
                livraison.setPrixNegocie(null);
                livraison.setTransporteurId(0);
                livraison.setLivreurIdProposant(null);
                livraison.setDateProposeeNegociation(null);
                livraison.setStatusNegociation("REFUSEE_NEGO");
                prepareNotification(
                        livraison,
                        actorId,
                        replyTarget,
                        "PRICE_NEGOTIATION_BAR",
                        "Proposition refusée",
                        "Votre proposition a été refusée pour la livraison " + safe(livraison.getReference()),
                        "REJECTED");
                break;
            case "COUNTER":
            case "COUNTER_PROPOSE":
                if (counterPrice == null || counterPrice <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "counterPrice est obligatoire pour une contre-proposition.");
                }
                livraison.setPrixNegocie(counterPrice);
                livraison.setProposedPrice(counterPrice);
                livraison.setDateProposeeNegociation(resolveNegotiatedDateTime(livraison, counterDateTime));
                livraison.setStatusNegociation("CONTRE_NEGO");
                prepareNotification(
                        livraison,
                        actorId,
                        replyTarget,
                        "PRICE_NEGOTIATION_BAR",
                        "Contre-proposition reçue",
                        "Une contre-proposition a été envoyée pour la livraison " + safe(livraison.getReference()),
                        "COUNTERED");
                break;
            case "MARK_READ":
                livraison.setNotificationSeen(true);
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action non valide: " + action);
        }

        Livraison saved = livraisonRepository.save(livraison);
        return toNotificationPayload(saved);
    }

    private void clearNotificationFields(Livraison livraison) {
        livraison.setNotificationFromUserId(null);
        livraison.setNotificationToUserId(null);
        livraison.setNotificationType(null);
        livraison.setNotificationTitle(null);
        livraison.setNotificationMessage(null);
        livraison.setNotificationStatus(null);
        livraison.setNotificationCreatedAt(null);
        livraison.setNotificationHandledAt(null);
        livraison.setNotificationSeen(false);
        livraison.setProposedPrice(null);
        livraison.setMinAllowedPrice(null);
        livraison.setMaxAllowedPrice(null);
    }

    private Integer resolveNotificationTarget(Livraison livraison, int actorId, Integer currentSender) {
        if (currentSender != null && currentSender > 0 && currentSender != actorId) {
            return currentSender;
        }
        if (livraison.getAgriculteurId() > 0 && livraison.getAgriculteurId() != actorId) {
            return livraison.getAgriculteurId();
        }
        if (livraison.getTransporteurId() > 0 && livraison.getTransporteurId() != actorId) {
            return livraison.getTransporteurId();
        }
        return livraison.getLivreurIdProposant();
    }

    private void prepareNotification(
            Livraison livraison,
            Integer fromUserId,
            Integer toUserId,
            String type,
            String title,
            String message,
            String status) {
        if (toUserId == null || toUserId <= 0) {
            return;
        }

        livraison.setNotificationFromUserId(fromUserId);
        livraison.setNotificationToUserId(toUserId);
        livraison.setNotificationType(type);
        livraison.setNotificationTitle(title);
        livraison.setNotificationMessage(message);
        livraison.setNotificationStatus(status);
        livraison.setNotificationCreatedAt(LocalDateTime.now());
        livraison.setNotificationHandledAt("PENDING".equalsIgnoreCase(status) ? null : LocalDateTime.now());
        livraison.setNotificationSeen(false);
        livraison.setProposedPrice(livraison.getPrixNegocie());
        livraison.setProposedDateTime(livraison.getDateProposeeNegociation());

        double base = round2(livraison.getPrix());
        livraison.setMinAllowedPrice(round2(base * (1 - MAX_NEGOTIATION_DELTA)));
        livraison.setMaxAllowedPrice(round2(base * (1 + MAX_NEGOTIATION_DELTA)));
    }

    private Map<String, Object> toNotificationPayload(Livraison livraison) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", livraison.getId());
        payload.put("livraisonId", livraison.getId());
        payload.put("fromUserId", livraison.getNotificationFromUserId());
        payload.put("toUserId", livraison.getNotificationToUserId());
        payload.put("type", safe(livraison.getNotificationType()));
        payload.put("title", safe(livraison.getNotificationTitle()));
        payload.put("message", safe(livraison.getNotificationMessage()));
        payload.put("proposedPrice", livraison.getProposedPrice() != null ? livraison.getProposedPrice() : livraison.getPrixNegocie());
        payload.put("preferredDateTime", livraison.getDatePreferenceAgriculteur() != null ? livraison.getDatePreferenceAgriculteur() : livraison.getDateDepart());
        payload.put("proposedDateTime", livraison.getProposedDateTime() != null ? livraison.getProposedDateTime() : livraison.getDateProposeeNegociation());
        payload.put("minAllowedPrice", livraison.getMinAllowedPrice());
        payload.put("maxAllowedPrice", livraison.getMaxAllowedPrice());
        payload.put("status", safe(livraison.getNotificationStatus()));
        payload.put("createdAt", livraison.getNotificationCreatedAt() != null
                ? livraison.getNotificationCreatedAt()
                : livraison.getDateCreation());
        payload.put("seen", livraison.isNotificationSeen());
        payload.put("reference", safe(livraison.getReference()));
        payload.put("pickupLat", livraison.getLatDepart());
        payload.put("pickupLng", livraison.getLngDepart());
        payload.put("dropoffLat", livraison.getLatArrivee());
        payload.put("dropoffLng", livraison.getLngArrivee());
        payload.put("currentLat", livraison.getLatActuelle());
        payload.put("currentLng", livraison.getLngActuelle());
        payload.put("trackingUrl", "/delivery/tracking?deliveryId=" + livraison.getId());
        return payload;
    }

    private LocalDateTime resolveNegotiatedDateTime(Livraison livraison, LocalDateTime candidate) {
        LocalDateTime resolved;
        if (candidate != null) {
            resolved = candidate;
        } else if (livraison.getDateProposeeNegociation() != null) {
            resolved = livraison.getDateProposeeNegociation();
        } else if (livraison.getDatePreferenceAgriculteur() != null) {
            resolved = livraison.getDatePreferenceAgriculteur();
        } else {
            resolved = livraison.getDateDepart();
        }
        return normalizeNegotiationDateForPlanning(resolved);
    }

    private LocalDateTime resolveAcceptedNegotiationDate(Livraison livraison) {
        LocalDateTime resolved;
        if (livraison.getDateProposeeNegociation() != null) {
            resolved = livraison.getDateProposeeNegociation();
        } else if (livraison.getDatePreferenceAgriculteur() != null) {
            resolved = livraison.getDatePreferenceAgriculteur();
        } else if (livraison.getDateLivraisonPrevue() != null) {
            resolved = livraison.getDateLivraisonPrevue();
        } else {
            resolved = livraison.getDateDepart();
        }
        return normalizeNegotiationDateForPlanning(resolved);
    }

    private Double resolveAcceptedNegotiationPrice(Livraison livraison) {
        if (livraison.getPrixNegocie() != null && livraison.getPrixNegocie() > 0) {
            return livraison.getPrixNegocie();
        }
        if (livraison.getProposedPrice() != null && livraison.getProposedPrice() > 0) {
            return livraison.getProposedPrice();
        }
        return null;
    }

    private LocalDateTime normalizeNegotiationDateForPlanning(LocalDateTime dateTime) {
        LocalDateTime fallback = LocalDateTime.now().plusHours(1).withSecond(0).withNano(0);
        if (dateTime == null) {
            return fallback;
        }
        if (dateTime.isBefore(LocalDateTime.now().minusMinutes(1))) {
            return fallback;
        }
        return dateTime;
    }
}
