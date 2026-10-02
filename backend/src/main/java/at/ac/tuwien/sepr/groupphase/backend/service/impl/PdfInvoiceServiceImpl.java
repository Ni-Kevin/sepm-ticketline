package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Order;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.TicketStatus;
import at.ac.tuwien.sepr.groupphase.backend.service.PdfInvoiceService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
public class PdfInvoiceServiceImpl implements PdfInvoiceService {

    private static final String COMPANY_NAME    = "Ticketline GmbH";
    private static final String COMPANY_ADDRESS = "Wiedner Hauptstraße 76, 1040 Wien, Austria";
    private static final String COMPANY_VAT     = "ATU12345678";
    private static final String COMPANY_EMAIL   = "office@ticketline.at";
    private static final BigDecimal VAT_RATE    = new BigDecimal("0.13");

    private static final Color PURPLE   = new Color(74, 21, 75);
    private static final Color GREY     = new Color(140, 140, 140);
    private static final Color DARK     = new Color(30, 30, 30);
    private static final Color DIVIDER  = new Color(220, 220, 220);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public byte[] generateInvoicePdf(Invoice invoice) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont   = new Font(Font.HELVETICA, 20, Font.BOLD,   PURPLE);
        Paragraph title = new Paragraph("INVOICE", titleFont);
        title.setSpacingAfter(4);
        doc.add(title);

        Font smallFont   = new Font(Font.HELVETICA,  8, Font.NORMAL, GREY);
        Paragraph basicInfo = new Paragraph(
            "Invoice No.: " + invoice.getInvoiceNumber()
                + "\nDate: " + invoice.getCreatedAt().format(DATE_FMT) + ".",
            smallFont);
        basicInfo.setSpacingAfter(20);
        doc.add(basicInfo);

        PdfPTable sellerBuyer = new PdfPTable(new float[]{1f, 1f});
        sellerBuyer.setWidthPercentage(100);
        sellerBuyer.setSpacingAfter(20);

        Font labelFont   = new Font(Font.HELVETICA,  9, Font.BOLD,   GREY);
        Font defaultFont = new Font(Font.HELVETICA, 10, Font.NORMAL, DARK);
        sellerBuyer.addCell(personCell("FROM", new String[]{
            COMPANY_NAME,
            COMPANY_ADDRESS,
            "USt-Id.Nr.: " + COMPANY_VAT,
            COMPANY_EMAIL
        }, labelFont, defaultFont));

        Order order = invoice.getOrder();
        ApplicationUser user = order.getUser();
        sellerBuyer.addCell(personCell("TO", new String[]{
            user.getFirstName() + " " + user.getLastName(),
            user.getEmail()
        }, labelFont, defaultFont));

        doc.add(sellerBuyer);

        String paymentMethod;
        switch (order.getPaymentMethod().name()) {
            case "CREDIT_CARD":
                paymentMethod = "Credit Card";
                break;
            case "KLARNA":
                paymentMethod = "Klarna";
                break;
            default:
                paymentMethod = "PayPal";
                break;
        }
        Paragraph orderRef = new Paragraph("Order #" + order.getId()
            + "\n" + order.getPurchaseDate().format(DATE_FMT) + "."
            + "\nPayment: " + paymentMethod, smallFont);
        orderRef.setSpacingAfter(10);
        doc.add(orderRef);

        PdfPTable items = new PdfPTable(new float[]{3.5f, 1f, 1f, 1f});
        items.setWidthPercentage(100);
        items.setSpacingAfter(10);

        addTableHeader(items, new String[]{"Event and Seat Details", "Quantity", "Unit Price", "Total"}, labelFont);

        BigDecimal purchasedTotal = BigDecimal.ZERO;
        if (order.getTickets() != null) {
            for (Ticket ticket : order.getTickets()) {
                if (ticket.getStatus() == TicketStatus.PURCHASED) {
                    String description = buildTicketDescription(ticket);
                    addTableRow(items, description,
                        formatEur(ticket.getFinalPrice()),
                        formatEur(ticket.getFinalPrice()),
                        defaultFont);
                    purchasedTotal = purchasedTotal.add(ticket.getFinalPrice());
                }
            }
        }

        doc.add(items);

        PdfPTable totals = new PdfPTable(new float[]{3f, 1f});
        totals.setWidthPercentage(60);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setSpacingAfter(20);

        BigDecimal net = purchasedTotal.divide(BigDecimal.ONE.add(VAT_RATE), 2, RoundingMode.HALF_UP);
        BigDecimal vat = purchasedTotal.subtract(net).setScale(2, RoundingMode.HALF_UP);
        Font boldFont    = new Font(Font.HELVETICA, 10, Font.BOLD,   DARK);
        addTotalRow(totals, "Subtotal (net)",       formatEur(net), defaultFont, defaultFont);
        addTotalRow(totals, "VAT (13%)",            formatEur(vat), defaultFont, defaultFont);
        addTotalRow(totals, "TOTAL",                 formatEur(purchasedTotal), boldFont,   boldFont);

        doc.add(totals);

        Paragraph footer = new Paragraph(
            "All prices include Austrian VAT at the applicable rate (13%).\n"
                + COMPANY_NAME + " · " + COMPANY_ADDRESS + " · " + COMPANY_VAT,
            smallFont);
        footer.setSpacingBefore(20);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }

    @Override
    public byte[] generateCancellationInvoicePdf(Invoice invoice) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont  = new Font(Font.HELVETICA, 20, Font.BOLD, PURPLE);
        Paragraph title = new Paragraph("CANCELLATION INVOICE", titleFont);
        title.setSpacingAfter(4);
        doc.add(title);

        Order order = invoice.getOrder();

        Font smallFont  = new Font(Font.HELVETICA,  8, Font.NORMAL, GREY);
        Paragraph basicInfo = new Paragraph(
            "Invoice No.: " + invoice.getInvoiceNumber()
                + "\nDate: " + invoice.getCreatedAt().format(DATE_FMT) + "."
                + "\nOriginal Order: #" + order.getId(),
            smallFont);
        basicInfo.setSpacingAfter(20);
        doc.add(basicInfo);

        PdfPTable sellerBuyer = new PdfPTable(new float[]{1f, 1f});
        sellerBuyer.setWidthPercentage(100);
        sellerBuyer.setSpacingAfter(20);

        Font labelFont   = new Font(Font.HELVETICA,  9, Font.BOLD,   GREY);
        Font defaultFont = new Font(Font.HELVETICA, 10, Font.NORMAL, DARK);
        sellerBuyer.addCell(personCell("FROM", new String[]{
            COMPANY_NAME, COMPANY_ADDRESS, "USt-Id.Nr.: " + COMPANY_VAT, COMPANY_EMAIL
        }, labelFont, defaultFont));
        ApplicationUser user = order.getUser();
        sellerBuyer.addCell(personCell("TO", new String[]{
            user.getFirstName() + " " + user.getLastName(), user.getEmail()
        }, labelFont, defaultFont));
        doc.add(sellerBuyer);

        PdfPTable items = new PdfPTable(new float[]{3.5f, 1f, 1f, 1f});
        items.setWidthPercentage(100);
        items.setSpacingAfter(10);
        addTableHeader(items, new String[]{"Event and Seat Details", "Quantity", "Unit Price", "Refund"}, labelFont);
        BigDecimal refundTotal = BigDecimal.ZERO;

        if (order.getTickets() != null) {
            for (Ticket ticket : order.getTickets()) {
                if (ticket.getStatus() == TicketStatus.CANCELLED) {
                    addTableRow(items, buildTicketDescription(ticket),
                        formatEur(ticket.getFinalPrice()),
                        formatEur(ticket.getFinalPrice()),
                        defaultFont);
                    refundTotal = refundTotal.add(ticket.getFinalPrice());
                }
            }
        }
        doc.add(items);

        PdfPTable totals = new PdfPTable(new float[]{3f, 1f});
        totals.setWidthPercentage(60);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setSpacingAfter(20);
        BigDecimal net = refundTotal.divide(BigDecimal.ONE.add(VAT_RATE), 2, RoundingMode.HALF_UP);
        BigDecimal vat = refundTotal.subtract(net).setScale(2, RoundingMode.HALF_UP);
        Font boldFont    = new Font(Font.HELVETICA, 10, Font.BOLD,   DARK);
        addTotalRow(totals, "Subtotal (net)",     formatEur(net),         defaultFont, defaultFont);
        addTotalRow(totals, "VAT (13%)",          formatEur(vat),         defaultFont, defaultFont);
        addTotalRow(totals, "TOTAL REFUND",        formatEur(refundTotal), boldFont,    boldFont);
        doc.add(totals);

        Paragraph footer = new Paragraph(
            "All prices include Austrian VAT at the applicable rate (13%). "
                + "Refund will be processed to your original payment method.\n"
                + COMPANY_NAME + " · " + COMPANY_ADDRESS + " · " + COMPANY_VAT,
            smallFont);
        footer.setSpacingBefore(20);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }

    private String buildTicketDescription(Ticket ticket) {
        StringBuilder sb = new StringBuilder();
        if (ticket.getPerformance() != null && ticket.getPerformance().getEvent() != null) {
            sb.append(ticket.getPerformance().getEvent().getTitle());
        }
        if (ticket.getPerformance() != null) {
            sb.append(" - ").append(ticket.getPerformance().getStartTime().format(DATE_FMT));
        }
        if (ticket.getSector() != null) {
            sb.append(" - ").append(ticket.getSector().getName());
        }
        if (ticket.getSeat() != null) {
            sb.append(" (Row ").append(ticket.getSeat().getRowNumber())
                .append(", Seat ").append(ticket.getSeat().getSeatNumber()).append(")");
        } else {
            sb.append(" (Standing)");
        }
        return sb.toString();
    }

    private PdfPCell personCell(String label, String[] lines, Font labelFont, Font normalFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(8);
        cell.addElement(new Paragraph(label, labelFont));
        for (String line : lines) {
            cell.addElement(new Paragraph(line, normalFont));
        }
        return cell;
    }

    private void addTableHeader(PdfPTable table, String[] headers, Font font) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(new Color(245, 245, 245));
            cell.setBorderColor(DIVIDER);
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String desc,
                             String unitPrice, String total, Font font) {
        for (String val : new String[]{desc, "1", unitPrice, total}) {
            PdfPCell cell = new PdfPCell(new Phrase(val, font));
            cell.setBorderColor(DIVIDER);
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private void addTotalRow(PdfPTable table, String label, String value,
                             Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(4);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private String formatEur(BigDecimal amount) {
        return String.format("€ %.2f", amount);
    }
}