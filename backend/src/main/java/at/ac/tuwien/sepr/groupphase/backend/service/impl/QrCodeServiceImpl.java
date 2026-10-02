package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.service.QrCodeService;
import io.nayuki.qrcodegen.QrCode;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    /**
     * Generates a QR code PNG from the given content string.
     * For a ticket, pass at minimum the bookingId.
     *
     * @param content the content that is to be used to generate the QR code
     */
    @Override
    public byte[] generateQrCodePng(String content) throws Exception {
        QrCode qrCode = QrCode.encodeText(content, QrCode.Ecc.MEDIUM);
        BufferedImage img = toImage(qrCode, 4, 10);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", out);
        out.flush();
        return out.toByteArray();
    }

    /**
     * A method for image creation for creating the QR code.
     *
     * @param qrCode encoding for image generation
     * @param scale the scale of the image
     * @param border the border of the image
     * @return a buffered image of the QR code
     */
    private BufferedImage toImage(QrCode qrCode, int scale, int border) {
        return toImage(qrCode, scale, border, 0xFFFFFF, 0x4A154B);
    }

    private BufferedImage toImage(QrCode qrCode, int scale, int border, int lightColor, int darkColor) {
        if (scale <= 0 || border < 0) {
            throw new IllegalArgumentException("Value out of range");
        }
        BufferedImage result = new BufferedImage(
            (qrCode.size + border * 2) * scale,
            (qrCode.size + border * 2) * scale,
            BufferedImage.TYPE_INT_RGB
        );
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                boolean color = qrCode.getModule(x / scale - border, y / scale - border);
                result.setRGB(x, y, color ? darkColor : lightColor);
            }
        }
        return result;
    }
}
