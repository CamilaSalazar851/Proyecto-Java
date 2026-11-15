package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.FinanceDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Delinquent;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.InvoiceData;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.AppConfig;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.InvoicePdfGenerator;
import com.littlehorizons.erp.util.JdbcHelper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Business rules for payments, electronic invoices and delinquency reports. */
public class FinanceController {

    private static final Path INVOICE_FOLDER = Path.of("invoices");
    private static final BigDecimal SIBLING_DISCOUNT = new BigDecimal("0.90");

    /** Suggested amount for a payment and an explanation shown to the user. */
    public record AmountSuggestion(BigDecimal amount, String note) {
    }

    /** Result of a confirmed payment. */
    public record PaymentResult(int paymentId, String receipt, String invoiceNumber, Path pdfPath) {
    }

    private final FinanceDao financeDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;

    public FinanceController(FinanceDao financeDao, StudentDao studentDao, GuardianDao guardianDao) {
        this.financeDao = financeDao;
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
    }

    public List<PaymentConcept> listConcepts() {
        try {
            return financeDao.findConcepts();
        } catch (SQLException e) {
            AppLogger.error("Error listing payment concepts", e);
            throw new AppException("No se pudo consultar el catalogo de conceptos.", e);
        }
    }

    public PaymentConcept requireConcept(int conceptId) {
        try {
            return financeDao.findConceptById(conceptId).orElseThrow(
                    () -> new AppException("No existe un concepto de pago activo con ID " + conceptId + "."));
        } catch (SQLException e) {
            AppLogger.error("Error finding concept " + conceptId, e);
            throw new AppException("No se pudo consultar el concepto.", e);
        }
    }

    /** Pension gets a 10% sibling discount when the principal guardian has two or more active children. */
    public AmountSuggestion suggestAmount(int studentId, PaymentConcept concept) {
        try {
            if ("PENSION".equals(concept.code()) && financeDao.countSiblingsOfPrincipalGuardian(studentId) >= 2) {
                BigDecimal discounted = concept.baseAmount().multiply(SIBLING_DISCOUNT).setScale(0, RoundingMode.HALF_UP);
                return new AmountSuggestion(discounted, "Descuento por hermanos del 10% aplicado.");
            }
            return new AmountSuggestion(concept.baseAmount(), "Valor base del concepto.");
        } catch (SQLException e) {
            AppLogger.error("Error calculating suggested amount for student " + studentId, e);
            throw new AppException("No se pudo calcular el valor sugerido.", e);
        }
    }

    /**
     * Confirms a payment: stores it, creates the invoice record with its simulated CUFE and writes the
     * PDF. Everything happens in one transaction, so a failed PDF also cancels the payment.
     */
    public PaymentResult registerPayment(int studentId, int conceptId, BigDecimal amount, String method,
                                         String period) {
        try {
            Student student = studentDao.findById(studentId).orElseThrow(
                    () -> new AppException("No existe un estudiante con ID " + studentId + "."));
            if (!"Activo".equals(student.getStatus())) {
                throw new AppException("El estudiante no esta activo (estado: " + student.getStatus() + ").");
            }
            requireConcept(conceptId);
            int guardianId = financeDao.findPrincipalGuardianId(studentId).orElseThrow(
                    () -> new AppException("El estudiante no tiene acudiente para facturar."));

            return JdbcHelper.inTransaction(conn -> {
                LocalDate today = LocalDate.now();
                String receipt = financeDao.nextReceiptNumber(conn, AppConfig.CURRENT_YEAR);
                int paymentId = financeDao.insertPayment(conn, studentId, guardianId, conceptId, amount, today,
                        method, receipt, AppConfig.CURRENT_YEAR, period);

                String invoiceNumber = financeDao.nextInvoiceNumber(conn);
                String cufe = InvoicePdfGenerator.computeCufe(invoiceNumber, today, amount, AppConfig.SCHOOL_NIT);
                String fileName = InvoicePdfGenerator.fileNameFor(invoiceNumber);
                Path pdfPath = INVOICE_FOLDER.resolve(fileName);
                financeDao.insertInvoice(conn, invoiceNumber, paymentId, today, cufe, fileName, pdfPath.toString());

                FinanceDao.InvoiceSource source = financeDao.findInvoiceSource(conn, paymentId).orElseThrow(
                        () -> new AppException("No se pudo reunir los datos de la factura."));
                InvoiceData data = buildInvoiceData(invoiceNumber, today, cufe, source);
                try {
                    InvoicePdfGenerator.generate(data, INVOICE_FOLDER);
                } catch (IOException e) {
                    AppLogger.error("Error writing invoice PDF " + fileName, e);
                    throw new AppException("No se pudo generar el PDF de la factura. El pago no fue registrado.", e);
                }
                return new PaymentResult(paymentId, receipt, invoiceNumber, pdfPath);
            });
        } catch (SQLException e) {
            AppLogger.error("Error registering payment for student " + studentId, e);
            throw new AppException("No se pudo registrar el pago. Revise error_log.txt.", e);
        }
    }

    private InvoiceData buildInvoiceData(String number, LocalDate date, String cufe, FinanceDao.InvoiceSource s) {
        String period = s.period() == null ? "" : " (Periodo " + s.period() + ")";
        String description = s.conceptName() + " - Estudiante: " + s.studentName() + period;
        BigDecimal subtotal = s.amount();
        BigDecimal tax = BigDecimal.ZERO;
        InvoiceData.Line line = new InvoiceData.Line(description, 1, s.amount(), subtotal);
        return new InvoiceData(number, date, cufe, s.guardianName(), s.guardianDocument(), s.guardianAddress(),
                s.receipt(), List.of(line), subtotal, tax, subtotal.add(tax));
    }

    public List<Payment> paymentsOfStudent(int studentId) {
        try {
            return financeDao.findPaymentsByStudent(studentId);
        } catch (SQLException e) {
            AppLogger.error("Error reading payments of student " + studentId, e);
            throw new AppException("No se pudo consultar el historial de pagos.", e);
        }
    }

    public Optional<Guardian> findGuardianByDocument(String document) {
        try {
            return guardianDao.findByDocument(document);
        } catch (SQLException e) {
            AppLogger.error("Error finding guardian " + document, e);
            throw new AppException("No se pudo consultar el acudiente.", e);
        }
    }

    public List<Payment> paymentsOfGuardian(int guardianId) {
        try {
            return financeDao.findPaymentsByGuardian(guardianId);
        } catch (SQLException e) {
            AppLogger.error("Error reading payments of guardian " + guardianId, e);
            throw new AppException("No se pudo consultar el historial de pagos del acudiente.", e);
        }
    }

    public List<Delinquent> delinquents() {
        try {
            return financeDao.findDelinquents();
        } catch (SQLException e) {
            AppLogger.error("Error building delinquency report", e);
            throw new AppException("No se pudo generar el reporte de morosos.", e);
        }
    }

    /** Returns "invoiceNumber|path" for a payment, checking that the PDF still exists on disk. */
    public Optional<String> findInvoice(int paymentId) {
        try {
            return financeDao.findInvoicePath(paymentId).map(value -> {
                String path = value.substring(value.indexOf('|') + 1);
                return Files.exists(Path.of(path)) ? value : value + " (archivo no encontrado en disco)";
            });
        } catch (SQLException e) {
            AppLogger.error("Error finding invoice of payment " + paymentId, e);
            throw new AppException("No se pudo consultar la factura.", e);
        }
    }
}
