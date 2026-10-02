package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;

public interface PdfInvoiceService {
    byte[] generateInvoicePdf(Invoice invoice) throws Exception;

    byte[] generateCancellationInvoicePdf(Invoice invoice) throws Exception;
}
