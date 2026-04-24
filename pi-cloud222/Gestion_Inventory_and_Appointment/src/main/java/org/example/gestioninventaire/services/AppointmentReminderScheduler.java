package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gestioninventaire.dtos.response.UserResponse;
import org.example.gestioninventaire.entities.Appointment;
import org.example.gestioninventaire.enums.AppointmentStatus;
import org.example.gestioninventaire.feigns.UserClient;
import org.example.gestioninventaire.repositories.AppointmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final UserClient userClient;
    private final TwilioSmsService twilioSmsService;

    @Value("${appointment.reminder.zone:Europe/Paris}")
    private String reminderZone;

    @Scheduled(cron = "${appointment.reminder.cron:0 0 9 * * *}", zone = "${appointment.reminder.zone:Europe/Paris}")
    @Transactional
    public void sendReminderForTomorrowAcceptedAppointments() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime start = tomorrow.atStartOfDay();
        LocalDateTime end = tomorrow.plusDays(1).atStartOfDay().minusNanos(1);

        List<Appointment> appointments = appointmentRepository
                .findByAppointmentStatusAndDateHeureBetweenAndSmsReminderSentAtIsNull(
                        AppointmentStatus.ACCEPTEE, start, end
                );

        if (appointments.isEmpty()) {
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH);

        for (Appointment appointment : appointments) {
            try {
                if (appointment.getFarmerId() == null) {
                    continue;
                }

                UserResponse farmer = userClient.getUserById(appointment.getFarmerId());
                String phone = farmer != null ? farmer.getTelephone() : null;
                if (phone == null || phone.isBlank()) {
                    continue;
                }

                String animalName = "votre animal";
                if (appointment.getAnimal() != null) {
                    if (appointment.getAnimal().getEspece() != null && !appointment.getAnimal().getEspece().isBlank()) {
                        animalName = appointment.getAnimal().getEspece();
                    } else if (appointment.getAnimal().getReference() != null && !appointment.getAnimal().getReference().isBlank()) {
                        animalName = appointment.getAnimal().getReference();
                    }
                }
                String rdvAt = appointment.getDateHeure() != null
                        ? appointment.getDateHeure().format(formatter)
                        : tomorrow.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                String sms = String.format(
                        "Rappel: votre rendez-vous vétérinaire accepté pour %s est prévu le %s. Motif: %s",
                        animalName,
                        rdvAt,
                        safe(appointment.getMotif())
                );

                boolean sent = twilioSmsService.sendSms(phone, sms);
                if (sent) {
                    appointment.setSmsReminderSentAt(LocalDateTime.now());
                    appointmentRepository.save(appointment);
                }
            } catch (Exception e) {
                log.error("Erreur lors du rappel SMS du rendez-vous {} (zone {}): {}",
                        appointment.getId(), reminderZone, e.getMessage(), e);
            }
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Non précisé" : value;
    }
}