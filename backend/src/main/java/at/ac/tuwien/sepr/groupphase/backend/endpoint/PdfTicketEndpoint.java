package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.PdfTicketService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class PdfTicketEndpoint {

    private final TicketRepository ticketRepository;
    private final PdfTicketService pdfTicketService;

    public PdfTicketEndpoint(TicketRepository ticketRepository, PdfTicketService pdfTicketService) {
        this.ticketRepository = ticketRepository;
        this.pdfTicketService = pdfTicketService;
    }

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getTicketPdf(@PathVariable("id") Long id) throws Exception {
        byte[] pdf = pdfTicketService.generateTicketPdf(
            ticketRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new NotFoundException("Ticket not found"))
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket-" + id + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}