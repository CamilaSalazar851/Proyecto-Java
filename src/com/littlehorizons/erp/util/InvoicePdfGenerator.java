package com.littlehorizons.erp.util;

import com.littlehorizons.erp.model.InvoiceData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the electronic invoice as a PDF without external libraries: the PDF syntax is written
 * by hand (one A4 page, Helvetica fonts). The CUFE and the QR code are SIMULATED for academic use.
 */
public final class InvoicePdfGenerator {

    private static final Charset PDF_CHARSET = Charset.forName("windows-1252");
    private static final int QR_SIZE = 29;

    private final StringBuilder content = new StringBuilder();

    private InvoicePdfGenerator() {
    }

    // ---------- public API ----------

    /** Writes the PDF in the given folder and returns its path. */
    public static Path generate(InvoiceData data, Path folder) throws IOException {
        Files.createDirectories(folder);
        Path file = folder.resolve(fileNameFor(data.invoiceNumber()));
        Files.write(file, new InvoicePdfGenerator().render(data));
        return file;
    }

    /** "FE-000123" becomes "factura_000123.pdf". */
    public static String fileNameFor(String invoiceNumber) {
        return "factura_" + invoiceNumber.replace("FE-", "") + ".pdf";
    }

    /** Simulated CUFE: SHA-384 of invoice number + issue date + total + school NIT. */
    public static String computeCufe(String invoiceNumber, LocalDate issueDate, BigDecimal total, String nit) {
        String raw = invoiceNumber + issueDate + total.toPlainString() + nit;
        return hex(digest("SHA-384", raw));
    }

    public static String qrPayload(InvoiceData data) {
        return "NumFac=" + data.invoiceNumber() + "\nFecFac=" + data.issueDate()
                + "\nNitFac=" + AppConfig.SCHOOL_NIT + "\nValTotFac=" + data.total().toPlainString()
                + "\nCUFE=" + data.cufe();
    }

    public static String formatMoney(BigDecimal amount) {
        return "$ " + String.format(Locale.GERMANY, "%,.0f", amount);
    }

    // ---------- page drawing ----------

    private byte[] render(InvoiceData d) throws IOException {
        // header band
        fillColor(0.10, 0.25, 0.45);
        rect(0, 762, 595, 80, true);
        fillColor(1, 1, 1);
        text(40, 812, true, 17, AppConfig.SCHOOL_NAME);
        text(40, 790, false, 11, "FACTURA ELECTRONICA DE VENTA");
        text(40, 774, false, 9, "Documento generado con fines academicos (simulacion DIAN)");

        // school data
        fillColor(0, 0, 0);
        text(40, 735, true, 10, "NIT: " + AppConfig.SCHOOL_NIT);
        text(40, 721, false, 10, AppConfig.SCHOOL_ADDRESS);
        text(40, 707, false, 10, "Tel: " + AppConfig.SCHOOL_PHONE + "   |   " + AppConfig.SCHOOL_EMAIL);

        // invoice number box
        strokeColor(0.10, 0.25, 0.45);
        rect(380, 695, 175, 52, false);
        text(390, 730, true, 10, "Factura No.");
        text(390, 714, true, 14, d.invoiceNumber());
        text(390, 700, false, 9, "Emision: " + d.issueDate());

        // client box
        line(40, 680, 555, 680);
        text(40, 665, true, 11, "DATOS DEL CLIENTE");
        text(40, 649, false, 10, "Cliente: " + d.clientName());
        text(40, 635, false, 10, "Documento: " + d.clientDocument());
        text(40, 621, false, 10, "Direccion: " + (d.clientAddress() == null ? "-" : d.clientAddress()));
        text(40, 607, false, 10, "Recibo de caja asociado: " + d.receiptNumber());

        // detail table header
        fillColor(0.90, 0.92, 0.96);
        rect(40, 570, 515, 20, true);
        fillColor(0, 0, 0);
        text(46, 576, true, 10, "Descripcion");
        text(330, 576, true, 10, "Cant.");
        textRight(450, 576, true, 10, "Valor unitario");
        textRight(550, 576, true, 10, "Subtotal");

        // detail rows
        int y = 554;
        for (InvoiceData.Line l : d.lines()) {
            List<String> parts = wrap(l.description(), 52);
            for (int i = 0; i < parts.size(); i++) {
                text(46, y, false, 10, parts.get(i));
                if (i == 0) {
                    text(336, y, false, 10, String.valueOf(l.quantity()));
                    textRight(450, y, false, 10, formatMoney(l.unitPrice()));
                    textRight(550, y, false, 10, formatMoney(l.subtotal()));
                }
                y -= 14;
            }
            line(40, y + 8, 555, y + 8);
            y -= 6;
        }

        // totals
        y -= 14;
        textRight(450, y, false, 10, "Subtotal:");
        textRight(550, y, false, 10, formatMoney(d.subtotal()));
        textRight(450, y - 15, false, 10, "IVA (0% - servicio educativo):");
        textRight(550, y - 15, false, 10, formatMoney(d.tax()));
        fillColor(0.90, 0.92, 0.96);
        rect(300, y - 42, 255, 20, true);
        fillColor(0, 0, 0);
        textRight(450, y - 36, true, 11, "TOTAL A PAGAR:");
        textRight(550, y - 36, true, 11, formatMoney(d.total()));

        // DIAN compliance block
        int top = 300;
        line(40, top + 14, 555, top + 14);
        text(40, top, true, 10, "ELEMENTOS DE CUMPLIMIENTO (SIMULADOS)");
        text(40, top - 16, true, 9, "CUFE:");
        text(40, top - 28, false, 8, d.cufe().substring(0, 48));
        text(40, top - 39, false, 8, d.cufe().substring(48));
        for (String part : wrap(AppConfig.DIAN_RESOLUTION, 90)) {
            top -= 11;
            text(40, top - 50, false, 8, part);
        }

        drawQr(qrPayload(d), 40, 80, 4.2);
        text(170, 150, true, 9, "Codigo QR simulado");
        text(170, 138, false, 8, "Contiene numero de factura, fecha, NIT,");
        text(170, 128, false, 8, "valor total y CUFE de esta factura.");

        fillColor(0.4, 0.4, 0.4);
        text(40, 40, false, 8, "Esta factura es una representacion grafica simulada y no tiene validez ante la DIAN.");

        return buildPdf();
    }

    private void drawQr(String payload, double x, double y, double module) {
        boolean[][] grid = qrMatrix(payload);
        fillColor(0, 0, 0);
        for (int row = 0; row < QR_SIZE; row++) {
            for (int col = 0; col < QR_SIZE; col++) {
                if (grid[row][col]) {
                    rect(x + col * module, y + (QR_SIZE - 1 - row) * module, module, module, true);
                }
            }
        }
    }

    /** QR-looking matrix: three finder squares plus data bits taken from a SHA-256 stream. */
    static boolean[][] qrMatrix(String payload) {
        boolean[][] grid = new boolean[QR_SIZE][QR_SIZE];
        byte[] bits = digest("SHA-256", payload);
        int counter = 0;
        for (int row = 0; row < QR_SIZE; row++) {
            for (int col = 0; col < QR_SIZE; col++) {
                if (counter / 8 >= bits.length) {
                    bits = digest("SHA-256", payload + row + col);
                    counter = 0;
                }
                grid[row][col] = ((bits[counter / 8] >> (counter % 8)) & 1) == 1;
                counter++;
            }
        }
        drawFinder(grid, 0, 0);
        drawFinder(grid, 0, QR_SIZE - 7);
        drawFinder(grid, QR_SIZE - 7, 0);
        return grid;
    }

    private static void drawFinder(boolean[][] grid, int top, int left) {
        for (int r = -1; r <= 7; r++) {
            for (int c = -1; c <= 7; c++) {
                int gr = top + r;
                int gc = left + c;
                if (gr < 0 || gc < 0 || gr >= QR_SIZE || gc >= QR_SIZE) {
                    continue;
                }
                boolean inside = r >= 0 && r <= 6 && c >= 0 && c <= 6;
                boolean ring = inside && (r == 0 || r == 6 || c == 0 || c == 6);
                boolean center = r >= 2 && r <= 4 && c >= 2 && c <= 4;
                grid[gr][gc] = ring || center;
            }
        }
    }

    // ---------- PDF primitives ----------

    private void text(double x, double y, boolean bold, int size, String value) {
        content.append("BT /").append(bold ? "F2" : "F1").append(' ').append(size).append(" Tf ")
               .append(num(x)).append(' ').append(num(y)).append(" Td (").append(escape(value)).append(") Tj ET\n");
    }

    private void textRight(double rightX, double y, boolean bold, int size, String value) {
        text(rightX - width(value, size), y, bold, size, value);
    }

    private void line(double x1, double y1, double x2, double y2) {
        strokeColor(0.7, 0.7, 0.7);
        content.append(num(x1)).append(' ').append(num(y1)).append(" m ")
               .append(num(x2)).append(' ').append(num(y2)).append(" l S\n");
    }

    private void rect(double x, double y, double w, double h, boolean fill) {
        content.append(num(x)).append(' ').append(num(y)).append(' ').append(num(w)).append(' ')
               .append(num(h)).append(" re ").append(fill ? "f" : "S").append('\n');
    }

    private void fillColor(double r, double g, double b) {
        content.append(num(r)).append(' ').append(num(g)).append(' ').append(num(b)).append(" rg\n");
    }

    private void strokeColor(double r, double g, double b) {
        content.append(num(r)).append(' ').append(num(g)).append(' ').append(num(b)).append(" RG\n");
    }

    /** Helvetica digits are 0.556 em wide, punctuation 0.278 em; letters are approximated. */
    private static double width(String value, int size) {
        double total = 0;
        for (char ch : value.toCharArray()) {
            if (Character.isDigit(ch) || ch == '$') {
                total += 0.556;
            } else if (ch == '.' || ch == ',' || ch == ' ' || ch == ':' || ch == '(' || ch == ')') {
                total += 0.278;
            } else {
                total += 0.55;
            }
        }
        return total * size;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static String num(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static List<String> wrap(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() + word.length() + 1 > maxChars && current.length() > 0) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        lines.add(current.toString());
        return lines;
    }

    private byte[] buildPdf() throws IOException {
        byte[] stream = content.toString().getBytes(PDF_CHARSET);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();

        out.write("%PDF-1.4\n".getBytes(PDF_CHARSET));
        String[] objects = {
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                + "/Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"
        };
        for (int i = 0; i < objects.length; i++) {
            offsets.add(out.size());
            out.write(((i + 1) + " 0 obj\n" + objects[i] + "\nendobj\n").getBytes(PDF_CHARSET));
        }
        offsets.add(out.size());
        out.write(("6 0 obj\n<< /Length " + stream.length + " >>\nstream\n").getBytes(PDF_CHARSET));
        out.write(stream);
        out.write("\nendstream\nendobj\n".getBytes(PDF_CHARSET));

        int xref = out.size();
        StringBuilder table = new StringBuilder("xref\n0 7\n0000000000 65535 f \n");
        for (int offset : offsets) {
            table.append(String.format("%010d 00000 n \n", offset));
        }
        table.append("trailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        out.write(table.toString().getBytes(PDF_CHARSET));
        return out.toByteArray();
    }

    private static byte[] digest(String algorithm, String value) {
        try {
            return MessageDigest.getInstance(algorithm).digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " is not available", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
