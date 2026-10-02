package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.Order;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.service.PdfTicketService;
import at.ac.tuwien.sepr.groupphase.backend.service.QrCodeService;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import java.awt.Color;

import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PdfTicketServiceImpl implements PdfTicketService {

    private final QrCodeService qrCodeService;

    public PdfTicketServiceImpl(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    /**
     * Generate a pdf of all tickets corresponding to that order.
     *
     * @param ticket ticket to be made into pdf
     * @return byte[] version of a pdf
     * @throws Exception when order isn't found
     */
    @Override
    public byte[] generateTicketPdf(Ticket ticket) throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rectangle pageSize = new Rectangle(PageSize.A4.getHeight(), PageSize.A4.getWidth());
        Document doc = new Document(pageSize, 0, 0, 0, 0);
        PdfWriter.getInstance(doc, out);
        doc.open();

        renderTicketPage(doc, ticket, ticket.getOrder());

        doc.close();
        return out.toByteArray();
    }

    /**
     * Generate a pdf of all tickets to be sent in the order confirmation email.
     *
     * @param order order from which to pull the tickets
     * @return byte[] version of a pdf
     * @throws Exception when order isn't found
     */
    @Override
    public byte[] generateOrderConfirmationTicketsPdf(Order order) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Rectangle pageSize = new Rectangle(PageSize.A4.getHeight(), PageSize.A4.getWidth());
        Document doc = new Document(pageSize, 0, 0, 0, 0);

        PdfWriter.getInstance(doc, out);
        doc.open();

        boolean first = true;

        for (Ticket ticket : order.getTickets()) {
            if (!first) {
                doc.newPage();
            }

            renderTicketPage(doc, ticket, order);
            first = false;
        }

        doc.close();
        return out.toByteArray();
    }

    private void renderTicketPage(Document doc, Ticket ticket, Order order) throws Exception {

        PdfPTable outer = new PdfPTable(new float[]{5f, 0.45f, 1.8f});
        outer.setWidthPercentage(100);
        outer.setExtendLastRow(true);

        PdfPTable leftBlock = new PdfPTable(1);
        leftBlock.setWidthPercentage(100);

        PdfPTable detailsGrid = new PdfPTable(new float[]{1f, 1f});
        detailsGrid.setWidthPercentage(100);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setColspan(2);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingLeft(10);
        titleCell.setPaddingTop(40);
        titleCell.setPaddingBottom(20);

        Performance perf  = ticket.getPerformance();
        Event event = perf.getEvent();

        Color brandColor = new Color(74, 21, 75);
        Font titleFont  = new Font(Font.HELVETICA, 20, Font.BOLD, brandColor);
        titleCell.addElement(new Paragraph(event.getTitle(), titleFont));
        detailsGrid.addCell(titleCell);

        Font labelFont  = new Font(Font.HELVETICA,  14, Font.BOLD,   new Color(140, 140, 140));
        Font valueFont  = new Font(Font.HELVETICA, 18, Font.NORMAL, new Color(30, 30, 30));

        String venueName = ticket.getSector().getHall().getVenue().getName();
        String venueCity = ticket.getSector().getHall().getVenue().getCity();

        String artists = perf.getArtists().stream()
            .map(Artist::getArtistName)
            .collect(Collectors.joining(", "));

        addDetailCell(detailsGrid, "ORDER",  "#" + order.getId(),               labelFont, valueFont);
        addDetailCell(detailsGrid, "VENUE",  venueName + ", " + venueCity,      labelFont, valueFont);
        addDetailCell(detailsGrid, "ARTIST", artists.isEmpty() ? "—" : artists, labelFont, valueFont);
        addDetailCell(detailsGrid, "SECTOR", ticket.getSector().getName(),       labelFont, valueFont);

        if (ticket.getSeat() != null) {
            addDetailCell(detailsGrid, "ROW",  String.valueOf(ticket.getSeat().getRowNumber()),  labelFont, valueFont);
            addDetailCell(detailsGrid, "SEAT", String.valueOf(ticket.getSeat().getSeatNumber()), labelFont, valueFont);
        } else {
            addDetailCell(detailsGrid, "STANDING", ticket.getSector().getName(), labelFont, valueFont);
            PdfPCell empty = new PdfPCell();
            empty.setBorder(Rectangle.NO_BORDER);
            detailsGrid.addCell(empty);
        }

        addDetailCell(detailsGrid, "PRICE",  String.format("€ %.2f", ticket.getFinalPrice()), labelFont, valueFont);
        addDetailCell(detailsGrid, "TICKET", "#" + ticket.getId(),                            labelFont, valueFont);

        PdfPCell detailsWrapper = new PdfPCell(detailsGrid);
        detailsWrapper.setBorder(Rectangle.BOTTOM);

        Color divider = new Color(220, 220, 220);

        detailsWrapper.setBorderColor(divider);
        detailsWrapper.setPaddingTop(20);
        detailsWrapper.setPaddingBottom(40);
        leftBlock.addCell(detailsWrapper);

        Image bgImage = Image.getInstance(
            Objects.requireNonNull(getClass().getClassLoader().getResource("gradient.png"))
        );

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setFixedHeight(75f);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setCellEvent((cell, rect, canvases) -> {
            try {
                bgImage.scaleAbsolute(rect.getWidth(), rect.getHeight());
                bgImage.setAbsolutePosition(rect.getLeft(), rect.getBottom());
                canvases[PdfPTable.BACKGROUNDCANVAS].addImage(bgImage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Paragraph logo = new Paragraph(
            "Ticketline 4.0",
            new Font(Font.HELVETICA, 36, Font.BOLD, Color.WHITE)
        );

        logo.setAlignment(Element.ALIGN_CENTER);
        logoCell.addElement(logo);

        leftBlock.addCell(logoCell);
        PdfPCell leftOuter = new PdfPCell(leftBlock);
        leftOuter.setBorder(Rectangle.RIGHT);
        leftOuter.setBorderColor(divider);
        leftOuter.setPadding(0);
        outer.addCell(leftOuter);

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE, dd MM yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        String dateStr = perf.getStartTime().format(dateFmt)
            + "   ·   "
            + perf.getStartTime().format(timeFmt);

        Font dateFont   = new Font(Font.HELVETICA,  15, Font.BOLD,   new Color(80, 80, 80));

        PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, dateFont));
        dateCell.setBorder(Rectangle.RIGHT);
        dateCell.setBorderColor(divider);
        dateCell.setRotation(90);
        dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        dateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        dateCell.setPadding(6);
        outer.addCell(dateCell);

        byte[] qrBytes = qrCodeService.generateQrCodePng(ticket.getQrCode());
        Image qrImage = Image.getInstance(qrBytes);
        qrImage.scaleToFit(120, 120);

        PdfPTable qrTable = new PdfPTable(1);
        qrTable.setWidthPercentage(100);

        PdfPCell qrImageCell = new PdfPCell(qrImage, true);
        qrImageCell.setBorder(Rectangle.NO_BORDER);
        qrImageCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrImageCell.setPaddingTop(16);
        qrImageCell.setPaddingBottom(4);
        qrTable.addCell(qrImageCell);

        Font footerFont = new Font(Font.HELVETICA,  15, Font.NORMAL, new Color(160, 160, 160));
        PdfPCell scanNoteCell = new PdfPCell(new Phrase("Scan at entrance", footerFont));
        scanNoteCell.setBorder(Rectangle.NO_BORDER);
        scanNoteCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrTable.addCell(scanNoteCell);

        PdfPCell ticketNoteCell = new PdfPCell(new Phrase("Ticket #" + ticket.getId(), footerFont));
        ticketNoteCell.setBorder(Rectangle.NO_BORDER);
        ticketNoteCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrTable.addCell(ticketNoteCell);

        PdfPCell qrCell = new PdfPCell(qrTable);
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        qrCell.setPadding(0);
        outer.addCell(qrCell);

        doc.add(outer);
    }

    private void addDetailCell(PdfPTable grid, String label, String value,
                               Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingLeft(14);
        cell.setPaddingRight(8);
        cell.setPaddingBottom(10);
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(value, valueFont));
        grid.addCell(cell);
    }

}
