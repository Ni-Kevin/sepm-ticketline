package at.ac.tuwien.sepr.groupphase.backend.service;

public interface QrCodeService {

    /**
     * Generates a QR code PNG from the given content string.
     * For a ticket, pass at minimum the bookingId.
     *
     * @param content the string representation of the content we want to encode in the QR code
     */
    byte[] generateQrCodePng(String content) throws Exception;

}
