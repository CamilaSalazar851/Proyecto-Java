package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Everything needed to draw one electronic invoice. */
public record InvoiceData(String invoiceNumber, LocalDate issueDate, String cufe,
                          String clientName, String clientDocument, String clientAddress,
                          String receiptNumber, List<Line> lines, BigDecimal subtotal,
                          BigDecimal tax, BigDecimal total) {

    public record Line(String description, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }
}
