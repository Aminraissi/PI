package org.exemple.paymentservice.services.impl;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemple.paymentservice.dtos.FactureDTO;
import org.exemple.paymentservice.entities.Facture;
import org.exemple.paymentservice.repositories.FactureRepository;
import org.exemple.paymentservice.services.FactureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.PageSize;
import com.lowagie.text.Document;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.UUID;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.exemple.paymentservice.entities.Paiement;
import org.exemple.paymentservice.repositories.PaiementRepository;
import org.springframework.web.client.RestTemplate;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Implementation of FactureService
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FactureServiceImpl implements FactureService {

    private final FactureRepository factureRepository;

    private final PaiementRepository paiementRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Facture createFactureAuto(Double montant) {
        log.info("Creating auto-generated Facture with amount: {}", montant);

        // Generate unique facture number: FAC-TIMESTAMP-RANDOMUUID
        String numero = generateFactureNumero();

        Facture facture = Facture.builder()
                .numero(numero)
                .date(LocalDate.now())
                .total(montant)
                .pdfUrl(null)  // Can be set later
                .build();

        Facture saved = factureRepository.save(facture);
        log.info("Facture created successfully with numero: {}", numero);

        return saved;
    }

    @Override
    public FactureDTO convertToDTO(Facture facture) {
        if (facture == null) {
            return null;
        }
        return FactureDTO.builder()
                .idFacture(facture.getIdFacture())
                .numero(facture.getNumero())
                .date(facture.getDate())
                .total(facture.getTotal())
                .pdfUrl(facture.getPdfUrl())
                .build();
    }

    @Override
    public Facture convertToEntity(FactureDTO factureDTO) {
        if (factureDTO == null) {
            return null;
        }
        return Facture.builder()
                .idFacture(factureDTO.getIdFacture())
                .numero(factureDTO.getNumero())
                .date(factureDTO.getDate())
                .total(factureDTO.getTotal())
                .pdfUrl(factureDTO.getPdfUrl())
                .build();
    }

    /**
     * Generate a unique facture number with pattern: FAC-YYYYMMDDHHMMSS-RANDOMUUID(8)
     * Example: FAC-20240316101530-A7F2E9C1
     */
    private String generateFactureNumero() {
        long timestamp = System.currentTimeMillis();
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "FAC-" + timestamp + "-" + randomPart;
    }

    @Override
    public byte[] generateFacturePdf(Long factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture not found"));

        Paiement paiement = paiementRepository.findByFactureIdFacture(factureId)
                .orElse(null);

        Map<String, Object> customer = null;

        if (paiement != null && paiement.getUserId() != null) {
            try {
                String userUrl = "http://localhost:8089/user/api/user/getUser/" + paiement.getUserId();
                System.out.println("Calling user service URL: " + userUrl);

                customer = restTemplate.getForObject(userUrl, Map.class);

                System.out.println("Customer response = " + customer);
            } catch (Exception e) {
                System.out.println("Could not load user details for invoice PDF: " + e.getMessage());
            }
        }

        Map<String, Object> orderDetail = null;
        List<Map<String, Object>> items = List.of();

        if (paiement != null && paiement.getCommandeId() != null) {
            try {
                String venteUrl = "http://localhost:8089/Vente/api/commande/admin/"
                        + paiement.getCommandeId()
                        + "/details";

                orderDetail = restTemplate.getForObject(venteUrl, Map.class);

                if (orderDetail != null && orderDetail.get("items") instanceof List) {
                    items = (List<Map<String, Object>>) orderDetail.get("items");
                }
            } catch (Exception e) {
                System.out.println("Could not load order details for invoice PDF: " + e.getMessage());
            }
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4, 45, 45, 40, 40);
            PdfWriter.getInstance(document, out);

            document.open();

            Color green = new Color(46, 125, 50);
            Color lightGreen = new Color(232, 245, 233);
            Color dark = new Color(31, 41, 55);
            Color gray = new Color(107, 114, 128);

            Font bigTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 42, dark);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, dark);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, dark);
            Font smallGrayFont = FontFactory.getFont(FontFactory.HELVETICA, 9, gray);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, dark);
            Font greenBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, green);

            // HEADER
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{2.2f, 1f});

            PdfPCell leftHeader = new PdfPCell();
            leftHeader.setBorder(Rectangle.NO_BORDER);

            try {
                InputStream logoStream = getClass().getResourceAsStream("/static/logo.png");
                if (logoStream != null) {
                    Image logo = Image.getInstance(logoStream.readAllBytes());
                    logo.scaleToFit(80, 80);
                    leftHeader.addElement(logo);
                }
            } catch (Exception ignored) {}

            Paragraph invoiceTitle = new Paragraph("Invoice", bigTitle);
            invoiceTitle.setSpacingBefore(8);
            leftHeader.addElement(invoiceTitle);

            PdfPCell rightHeader = new PdfPCell();
            rightHeader.setBorder(Rectangle.NO_BORDER);
            rightHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph date = new Paragraph(String.valueOf(facture.getDate()), normalFont);
            date.setAlignment(Element.ALIGN_RIGHT);

            Paragraph number = new Paragraph("Invoice No. " + facture.getNumero(), titleFont);
            number.setAlignment(Element.ALIGN_RIGHT);

            rightHeader.addElement(date);
            rightHeader.addElement(number);

            header.addCell(leftHeader);
            header.addCell(rightHeader);
            document.add(header);

            addSpacer(document, 18);
            addLine(document);
            addSpacer(document, 18);

            // BILLED TO
            Paragraph billedTitle = new Paragraph("Billed to:", boldFont);
            document.add(billedTitle);

            if (customer != null) {
                String nom = String.valueOf(customer.getOrDefault("nom", ""));
                String prenom = String.valueOf(customer.getOrDefault("prenom", ""));
                String email = String.valueOf(customer.getOrDefault("email", ""));
                String telephone = String.valueOf(customer.getOrDefault("telephone", ""));

                String fullName = (prenom + " " + nom).trim();

                if (!fullName.isBlank() && !"null".equals(fullName)) {
                    document.add(new Paragraph(fullName, normalFont));
                } else {
                    document.add(new Paragraph("GreenRoots Customer #" + paiement.getUserId(), normalFont));
                }

                if (!email.isBlank() && !"null".equals(email)) {
                    document.add(new Paragraph("Email: " + email, normalFont));
                }

                if (!telephone.isBlank() && !"null".equals(telephone)) {
                    document.add(new Paragraph("Phone: " + telephone, normalFont));
                }

                document.add(new Paragraph("Tunisia", normalFont));
            } else {
                Long userId = paiement != null ? paiement.getUserId() : null;
                document.add(new Paragraph("GreenRoots Customer" + (userId != null ? " #" + userId : ""), normalFont));
                document.add(new Paragraph("Tunisia", normalFont));
            }

            addSpacer(document, 20);
            addLine(document);
            addSpacer(document, 30);

            // ITEMS TABLE
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 1.2f, 1.2f, 1.4f});

            addTableHeader(table, "Description", headerFont, green);
            addTableHeader(table, "Unit Price", headerFont, green);
            addTableHeader(table, "Quantity", headerFont, green);
            addTableHeader(table, "Amount", headerFont, green);

            double calculatedSubtotal = 0.0;

            if (!items.isEmpty()) {
                for (Map<String, Object> item : items) {
                    String nom = String.valueOf(item.getOrDefault("nom", "Product"));
                    double prixUnitaire = toDouble(item.get("prixUnitaire"));
                    double quantite = toDouble(item.get("quantite"));
                    double sousTotal = toDouble(item.get("sousTotal"));

                    calculatedSubtotal += sousTotal;

                    addTableCell(table, nom, normalFont);
                    addTableCell(table, formatMoney(prixUnitaire) + " DT", normalFont);
                    addTableCell(table, formatQty(quantite) + " KG", normalFont);
                    addTableCell(table, formatMoney(sousTotal) + " DT", normalFont);
                }
            } else {
                addTableCell(table, "Paid order", normalFont);
                addTableCell(table, "-", normalFont);
                addTableCell(table, "-", normalFont);
                addTableCell(table, formatMoney(facture.getTotal()) + " DT", normalFont);
                calculatedSubtotal = facture.getTotal();
            }

            document.add(table);

            addSpacer(document, 18);

            // TOTALS
            double total = facture.getTotal() != null ? facture.getTotal() : 0.0;

            double subtotal = orderDetail != null && orderDetail.get("sousTotal") != null
                    ? toDouble(orderDetail.get("sousTotal"))
                    : calculatedSubtotal;

            double commission = orderDetail != null && orderDetail.get("commission") != null
                    ? toDouble(orderDetail.get("commission"))
                    : Math.max(0, total - subtotal);

            double tip = orderDetail != null && orderDetail.get("tip") != null
                    ? toDouble(orderDetail.get("tip"))
                    : 0.0;

            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(45);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.setWidths(new float[]{1.4f, 1f});

            addTotalRow(totals, "Subtotal", formatMoney(subtotal) + " DT", normalFont, false);
            addTotalRow(totals, "Commission", formatMoney(commission) + " DT", normalFont, false);

            if (tip > 0) {
                addTotalRow(totals, "Tip", formatMoney(tip) + " DT", normalFont, false);
            }

            addTotalRow(totals, "Total", formatMoney(total) + " DT", greenBold, true);

            document.add(totals);

            addSpacer(document, 40);
            addLine(document);
            addSpacer(document, 20);

            // PAYMENT INFO
            PdfPTable bottom = new PdfPTable(2);
            bottom.setWidthPercentage(100);
            bottom.setWidths(new float[]{1f, 1f});

            PdfPCell paymentInfo = new PdfPCell();
            paymentInfo.setBorder(Rectangle.NO_BORDER);
            paymentInfo.addElement(new Paragraph("Payment Information", boldFont));

            if (paiement != null) {
                paymentInfo.addElement(new Paragraph("Method: " + paiement.getMethode(), normalFont));
                paymentInfo.addElement(new Paragraph("Reference: " + paiement.getReference(), normalFont));
                paymentInfo.addElement(new Paragraph("Payment date: " + paiement.getDatePaiement(), normalFont));
                paymentInfo.addElement(new Paragraph("Status: " + paiement.getStatut(), normalFont));
            } else {
                paymentInfo.addElement(new Paragraph("Payment information unavailable", normalFont));
            }

            PdfPCell companyInfo = new PdfPCell();
            companyInfo.setBorder(Rectangle.NO_BORDER);
            companyInfo.addElement(new Paragraph("GreenRoots", boldFont));
            companyInfo.addElement(new Paragraph("Agricultural marketplace", normalFont));
            companyInfo.addElement(new Paragraph("Tunisia", normalFont));
            companyInfo.addElement(new Paragraph("Thank you for using GreenRoots!", greenBold));

            bottom.addCell(paymentInfo);
            bottom.addCell(companyInfo);

            document.add(bottom);

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private void addSpacer(Document document, float height) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(height);
        document.add(spacer);
    }

    private void addLine(Document document) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(180, 180, 180));
        cell.setFixedHeight(1f);

        line.addCell(cell);
        document.add(line);
    }

    private void addTableHeader(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(9);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(9);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(220, 220, 220));
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font font, boolean highlight) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));

        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        labelCell.setPadding(8);
        valueCell.setPadding(8);

        labelCell.setBorder(highlight ? Rectangle.TOP : Rectangle.NO_BORDER);
        valueCell.setBorder(highlight ? Rectangle.TOP : Rectangle.NO_BORDER);

        if (highlight) {
            labelCell.setBorderColor(new Color(180, 180, 180));
            valueCell.setBorderColor(new Color(180, 180, 180));
        }

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String formatMoney(double value) {
        return String.format("%.2f", value);
    }

    private String formatQty(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }
        return String.format("%.2f", value);
    }
}

