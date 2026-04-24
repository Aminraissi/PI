package org.example.gestionevenement.Services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import lombok.AllArgsConstructor;
import org.example.gestionevenement.Repositories.TicketRepo;
import org.example.gestionevenement.entities.Reservation;
import org.example.gestionevenement.entities.Ticket;
import org.springframework.stereotype.Service;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class TicketServiceImp implements ITicket{

    private TicketRepo ticketRepo;
    @Override
    public List<Ticket> getAllTickets() {
        return ticketRepo.findAll();
    }

    @Override
    public Ticket updateTicket(Ticket ticket) {
        return ticketRepo.save(ticket);
    }

    @Override
    public Ticket addTicket(Ticket ticket) {
        return ticketRepo.save(ticket);
    }

    @Override
    public Ticket getTicket(int idTicket) {
        return ticketRepo.findById(idTicket).orElse(null);
    }

    @Override
    public void removeTicket(int idTicket) {
        ticketRepo.deleteById(idTicket);
    }

    @Override
    public Ticket generateTicket(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        try {
            String codeTicket = "TKT-" + UUID.randomUUID().toString().toUpperCase().substring(0, 12);

            String qrContent = buildQrContent(reservation, codeTicket);

            byte[] qrImageBytes = generateQrCodeImage(qrContent, 300);

            Ticket ticket = new Ticket();
            ticket.setCodeTicket(codeTicket);
            ticket.setDateCreation(LocalDateTime.now());
            ticket.setQrCode(qrContent);
            ticket.setUsed(false);
            ticket.setReservation(reservation);

            return ticketRepo.save(ticket);

        } catch (Exception e) {
            return null;
        }
    }
    private String buildQrContent(Reservation reservation, String codeTicket) {
        return String.format(
                "TICKET:%s|RESERVATION:%d|USER:%d|AMOUNT:%.2f TND|DATE:%s",
                codeTicket,
                reservation.getId(),
                reservation.getId_user(),
                reservation.getMontant(),
                LocalDateTime.now().toString()
        );
    }

    private byte[] generateQrCodeImage(String content, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
