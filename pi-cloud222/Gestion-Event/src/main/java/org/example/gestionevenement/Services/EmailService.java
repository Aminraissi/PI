package org.example.gestionevenement.Services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.example.gestionevenement.entities.Reservation;
import org.example.gestionevenement.entities.Ticket;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;


@Service
public class EmailService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTicketConfirmation(String toEmail, Reservation reservation, Ticket ticket)
            throws MessagingException, UnsupportedEncodingException {

        byte[] qrImageBytes;
        try {
            qrImageBytes = generateQRCodeImage(ticket.getQrCode(), 200, 200);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code image", e);
        }

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

        helper.setFrom("noreply@GreenRoots.com", "GreenRoots");
        helper.setTo(toEmail);
        helper.setSubject("Confirm Reservation – " + ticket.getCodeTicket());

        helper.setText(buildHtmlBody(reservation, ticket), true);
        helper.addInline("qrcode", new ByteArrayResource(qrImageBytes), "image/png");

        mailSender.send(message);
    }

    private byte[] generateQRCodeImage(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        var bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

    private String buildHtmlBody(Reservation reservation, Ticket ticket) {

        String date = ticket.getDateCreation() != null
                ? ticket.getDateCreation().format(DATE_FMT)
                : "—";

        String paymentDate = reservation.getDateInscription() != null
                ? reservation.getDateInscription().format(DATE_FMT)
                : "—";

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>Booking Confirmed</title>
                </head>
                <body style="margin:0;padding:0;background:#F4F6F9;font-family:'Segoe UI',Arial,sans-serif;">

                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#F4F6F9;padding:40px 0;">
                    <tr><td align="center">

                      <table width="600" cellpadding="0" cellspacing="0"
                             style="background:#FFFFFF;border-radius:12px;overflow:hidden;
                                    box-shadow:0 4px 20px rgba(0,0,0,0.08);">

                       <tr>
                               <td style="background:linear-gradient(135deg,#14532d 0%%,#052e16 100%%);
                                          padding:36px 40px;
                                          text-align:center;">
                
                                 <p style="margin:0 0 6px;color:rgba(255,255,255,.75);
                                           font-size:12px;letter-spacing:2px;text-transform:uppercase;">
                                   GreenRoots
                                 </p>
                                 <h1 style="margin:0;color:#FFFFFF;font-size:26px;font-weight:700;">
                                   Booking Confirmed ✓
                                 </h1>
                               </td>
                             </tr>
                        <tr>
                          <td style="padding:40px;">

                            <p style="margin:0 0 24px;color:#333;font-size:15px;line-height:1.6;">
                              Hello,<br/>
                              Your payment has been successfully received. Here is a summary of your reservation along with your QR ticket to present at the entrance.
                            </p>

                            <table width="100%%" cellpadding="0" cellspacing="0"
                                   style="background:#F8FAFF;border:1px solid #E3EAFF;
                                          border-radius:8px;margin-bottom:32px;">
                              <tr>
                                <td style="padding:16px 20px;border-bottom:1px solid #E3EAFF;">
                                  <p style="margin:0;font-size:11px;color:#6B7280;
                                            text-transform:uppercase;letter-spacing:1px;">
                                    Booking Details
                                  </p>
                                </td>
                              </tr>

                              %s

                            </table>

                            <table width="100%%" cellpadding="0" cellspacing="0"
                                   style="margin-bottom:32px;">
                              <tr>
                                <td align="center"
                                    style="background:#FAFAFA;border:2px dashed #CBD5E0;
                                           border-radius:8px;padding:28px;">
                                  <p style="margin:0 0 16px;color:#374151;font-size:13px;
                                            font-weight:600;letter-spacing:0.5px;">
                                    YOUR ACCESS QR CODE
                                  </p>
                                  <img src="cid:qrcode" alt="QR Code"
                                       width="200" height="200"
                                       style="display:block;border-radius:4px;"/>
                                  <p style="margin:14px 0 0;color:#6B7280;font-size:12px;">
                                    Code : <strong style="color:#1A73E8;letter-spacing:1px;">%s</strong>
                                  </p>
                                </td>
                              </tr>
                            </table>

                            <table width="100%%" cellpadding="0" cellspacing="0"
                                   style="background:#FFF8E1;border-left:4px solid #16a34a;border-radius:4px;margin-bottom:24px;">
                              <tr>
                                  <td style="padding:14px 18px;background:#ecfdf5;border-left:4px solid #16a34a; border-radius:4px;">
                                       <p style="margin:0;color:#065f46;font-size:13px;line-height:1.6;">
                                          <strong>Important:</strong> Please present this QR code at the event entrance. It is single-use and personal.
                                       </p>
                                     </td>
                                   </tr>
                            </table>

                            <p style="margin:0;color:#9CA3AF;font-size:12px;text-align:center;">
                              For any questions, contact us at
                              <a href="mailto:GreenRoots@gmail.com"
                                 style="color:#1A73E8;text-decoration:none;">
                                GreenRoots@gmail.com
                              </a>
                            </p>

                          </td>
                        </tr>
                        <tr>
                          <td style="background:#F9FAFB;border-top:1px solid #E5E7EB;
                                     padding:20px 40px;text-align:center;">
                            <p style="margin:0;color:#9CA3AF;font-size:11px;">
                              © 2026 GreenRoots · All rights reserved <br/>
                              This email was automatically generated, please do not reply.
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(buildRows(reservation, ticket, paymentDate), ticket.getCodeTicket());
    }

    private String buildRows(Reservation reservation, Ticket ticket,
                             String paymentDate) {

        return row("Ticket ID",       ticket.getCodeTicket())
                + row("Amount paid",    String.format("%.2f TND ", reservation.getMontant()))
                + row("Payment date", paymentDate)
                + row("Status",
                "<span style='color:#059669;font-weight:700;'>✓ PAID</span>");
    }

    private String row(String label, String value) {
        return """
               <tr>
                 <td style="padding:12px 20px;border-bottom:1px solid #E3EAFF;
                             color:#6B7280;font-size:13px;width:45%%;">%s</td>
                 <td style="padding:12px 20px;border-bottom:1px solid #E3EAFF;
                             color:#111827;font-size:13px;font-weight:600;">%s</td>
               </tr>
               """.formatted(label, value);
    }
}