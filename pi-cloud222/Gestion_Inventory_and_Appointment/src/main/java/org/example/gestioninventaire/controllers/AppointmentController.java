package org.example.gestioninventaire.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateAppointmentRequest;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.dtos.response.AppointmentResponse;
import org.example.gestioninventaire.dtos.response.AppointmentStatsResponse;
import org.example.gestioninventaire.dtos.response.SmsTestResponse;
import org.example.gestioninventaire.mappers.AppointmentMapper;
import org.example.gestioninventaire.repositories.AppointmentRepository;
import org.example.gestioninventaire.services.AppointmentService;
import org.example.gestioninventaire.services.TwilioSmsService;
import org.example.gestioninventaire.util.JwtUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final JwtUtils jwtUtils;
    private final TwilioSmsService twilioSmsService;

    @PostMapping
    public ApiResponse<AppointmentResponse> create(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateAppointmentRequest request) {
        Long farmerId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<AppointmentResponse>builder()
                .message("Rendez-vous cree avec succes")
                .data(appointmentService.createAppointment(request, farmerId))
                .build();
    }

    @GetMapping("/farmer/{farmerId}")
    public ApiResponse<List<AppointmentResponse>> getByFarmer(@PathVariable Long farmerId) {
        List<AppointmentResponse> list = appointmentRepository.findByFarmerId(farmerId)
                .stream().map(appointmentMapper::toAppointmentResponse).collect(Collectors.toList());
        return ApiResponse.<List<AppointmentResponse>>builder()
                .message("Rendez-vous recuperes").data(list).build();
    }

    @GetMapping("/vet/{vetId}")
    public ApiResponse<List<AppointmentResponse>> getByVet(@PathVariable Long vetId) {
        List<AppointmentResponse> list = appointmentRepository.findByVeterinarianId(vetId)
                .stream().map(appointmentMapper::toAppointmentResponse).collect(Collectors.toList());
        return ApiResponse.<List<AppointmentResponse>>builder()
                .message("Rendez-vous recuperes").data(list).build();
    }

    @PutMapping("/{id}/accept")
    public ApiResponse<AppointmentResponse> accept(@PathVariable Long id) {
        return ApiResponse.<AppointmentResponse>builder()
                .message("Accepte").data(appointmentService.acceptAppointment(id)).build();
    }

    @PutMapping("/{id}/refuse")
    public ApiResponse<AppointmentResponse> refuse(@PathVariable Long id, @RequestParam String reason) {
        return ApiResponse.<AppointmentResponse>builder()
                .message("Refuse").data(appointmentService.refuseAppointment(id, reason)).build();
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<AppointmentResponse> cancel(@PathVariable Long id) {
        return ApiResponse.<AppointmentResponse>builder()
                .message("Annule").data(appointmentService.cancelAppointment(id)).build();
    }

    @GetMapping("/vet/{vetId}/stats")
    public ApiResponse<AppointmentStatsResponse> getVetStats(@PathVariable Long vetId) {
        return ApiResponse.<AppointmentStatsResponse>builder()
                .message("Statistiques veterinaire recuperees")
                .data(appointmentService.getVetStats(vetId))
                .build();
    }

    @GetMapping("/farmer/{farmerId}/stats")
    public ApiResponse<AppointmentStatsResponse> getFarmerStats(@PathVariable Long farmerId) {
        return ApiResponse.<AppointmentStatsResponse>builder()
                .message("Statistiques agriculteur recuperees")
                .data(appointmentService.getFarmerStats(farmerId))
                .build();
    }

    @PostMapping("/test-sms")
    public ApiResponse<SmsTestResponse> testSms(
            @RequestParam String to,
            @RequestParam(defaultValue = "Test SMS Twilio depuis Gestion-Inventaire.") String message) {
        SmsTestResponse result = twilioSmsService.sendSmsDetailed(to, message);
        return ApiResponse.<SmsTestResponse>builder()
                .message(result.isSuccess() ? "SMS test envoye." : "Echec SMS test.")
                .data(result)
                .build();
    }
}