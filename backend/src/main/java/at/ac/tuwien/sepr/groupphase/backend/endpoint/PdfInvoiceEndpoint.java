package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.InvoiceType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Order;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.InvoiceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.OrderRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.PdfInvoiceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class PdfInvoiceEndpoint {

    private final OrderRepository orderRepository;
    private final PdfInvoiceService pdfInvoiceService;
    private final InvoiceRepository invoiceRepository;

    public PdfInvoiceEndpoint(OrderRepository orderRepository, PdfInvoiceService pdfInvoiceService, InvoiceRepository invoiceRepository) {
        this.orderRepository = orderRepository;
        this.pdfInvoiceService = pdfInvoiceService;
        this.invoiceRepository = invoiceRepository;
    }

    @Secured("ROLE_USER")
    @GetMapping("/by-ticket/{ticketId}/invoice")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable(name = "ticketId") Long ticketId) throws Exception {
        Order order = orderRepository.findByTicketIdWithTickets(ticketId)
            .orElseThrow(() -> new NotFoundException("Order not found for ticket " + ticketId));

        Invoice invoice = invoiceRepository.findFirstByOrderIdAndTypeOrderByIdDesc(order.getId(), InvoiceType.PURCHASED)
            .orElseThrow(() -> new NotFoundException("No invoice found for order"));

        Invoice fullInvoice = invoiceRepository.findByIdWithAllRelations(invoice.getId())
            .orElseThrow(() -> new NotFoundException("Invoice not found"));

        byte[] pdf = pdfInvoiceService.generateInvoicePdf(fullInvoice);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + order.getId() + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @Secured("ROLE_USER")
    @GetMapping("/by-ticket/{ticketId}/cancellation-invoice")
    public ResponseEntity<byte[]> getCancellationInvoicePdf(@PathVariable(name = "ticketId") Long ticketId) throws Exception {
        Order order = orderRepository.findByTicketIdWithTickets(ticketId)
            .orElseThrow(() -> new NotFoundException("Order not found for ticket " + ticketId));

        Invoice invoice = invoiceRepository.findFirstByOrderIdAndTypeOrderByIdDesc(order.getId(), InvoiceType.CANCELLED)
            .orElseThrow(() -> new NotFoundException("No cancellation invoice found for order"));

        Invoice fullInvoice = invoiceRepository.findByIdWithAllRelations(invoice.getId())
            .orElseThrow(() -> new NotFoundException("Invoice not found"));

        byte[] pdf = pdfInvoiceService.generateCancellationInvoicePdf(fullInvoice);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cancellation-invoice-" + order.getId() + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}