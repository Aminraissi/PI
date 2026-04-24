package org.example.gestionvente.scheduler;
import lombok.RequiredArgsConstructor;
import org.example.gestionvente.Services.IReservationVisiteService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationScheduler {

    private final IReservationVisiteService reservationService;

    public ReservationScheduler(IReservationVisiteService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(fixedRate = 60000)
    public void updateExpiredReservations() {
        reservationService.updateExpiredReservations();
    }
}
