package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.model.Delinquent;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.OperationCancelledException;
import com.littlehorizons.erp.util.InvoicePdfGenerator;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Console screens for payments, invoices and delinquency. */
public class FinanceView {

    private final FinanceController controller;

    public FinanceView(FinanceController controller) {
        this.controller = controller;
    }

    public void showMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== GESTION FINANCIERA ===");
            System.out.println("(escriba \'cancelar\' en cualquier campo para volver al menu)");
            System.out.println("1. Catalogo de conceptos de pago");
            System.out.println("2. Registrar pago y generar factura PDF");
            System.out.println("3. Historial de pagos de un estudiante");
            System.out.println("4. Historial de pagos de un acudiente");
            System.out.println("5. Reporte de morosos");
            System.out.println("6. Ubicar la factura de un pago");
            System.out.println("7. Volver al menu principal");

            int option = ConsoleInput.readInt("Seleccione una opcion: ", 1, 7);
            try {
                switch (option) {
                    case 1 -> printConcepts();
                    case 2 -> registerPayment();
                    case 3 -> studentHistory();
                    case 4 -> guardianHistory();
                    case 5 -> printDelinquents();
                    case 6 -> locateInvoice();
                    default -> back = true;
                }
            } catch (OperationCancelledException e) {
                System.out.println("\nOperacion cancelada. Volviendo al menu.");
            } catch (AppException e) {
                System.out.println("\n[ERROR] " + e.getMessage());
            }
            if (!back) {
                ConsoleInput.pause();
            }
        }
    }

    private void printConcepts() {
        System.out.printf("%n%-4s %-14s %-28s %s%n", "ID", "Codigo", "Concepto", "Valor base");
        controller.listConcepts().forEach(c -> System.out.printf("%-4d %-14s %-28s %s%n", c.id(), c.code(),
                c.name(), InvoicePdfGenerator.formatMoney(c.baseAmount())));
    }

    private void registerPayment() {
        int studentId = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        printConcepts();
        int conceptId = ConsoleInput.readInt("\nID del concepto: ", 1, Integer.MAX_VALUE);
        PaymentConcept concept = controller.requireConcept(conceptId);

        FinanceController.AmountSuggestion suggestion = controller.suggestAmount(studentId, concept);
        System.out.println(suggestion.note() + " Valor sugerido: " + InvoicePdfGenerator.formatMoney(suggestion.amount()));
        BigDecimal custom = ConsoleInput.readMoneyOrDefault(
                "Monto a pagar (Enter para usar el sugerido; escriba otro valor para una beca o ajuste): ");
        BigDecimal amount = custom == null ? suggestion.amount() : custom;

        String method = ConsoleInput.readChoice("Metodo de pago:", Payment.METHODS);
        String period = ConsoleInput.readChoice("Periodo al que aplica:", Payment.PERIODS);

        if (!ConsoleInput.confirm("Confirma el pago de " + InvoicePdfGenerator.formatMoney(amount) + "?")) {
            System.out.println("\nOperacion cancelada.");
            return;
        }
        FinanceController.PaymentResult result = controller.registerPayment(studentId, conceptId, amount, method, period);
        System.out.println("\n[OK] Pago registrado. Recibo: " + result.receipt());
        System.out.println("[OK] Factura " + result.invoiceNumber() + " generada en: " + result.pdfPath().toAbsolutePath());
    }

    private void studentHistory() {
        int studentId = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        printPayments(controller.paymentsOfStudent(studentId));
    }

    private void guardianHistory() {
        String document = ConsoleInput.readRequired("Numero de documento del acudiente: ", 20);
        controller.findGuardianByDocument(document).ifPresentOrElse(g -> {
            System.out.println("\nPagos de los estudiantes a cargo de " + g.fullName() + ":");
            printPayments(controller.paymentsOfGuardian(g.id()));
        }, () -> System.out.println("\n[!] No existe un acudiente con ese documento."));
    }

    private void printPayments(List<Payment> payments) {
        if (payments.isEmpty()) {
            System.out.println("\nNo hay pagos registrados.");
            return;
        }
        System.out.printf("%n%-6s %-15s %-22s %-24s %-14s %-11s %s%n", "ID", "Recibo", "Estudiante", "Concepto",
                "Monto", "Fecha", "Metodo");
        System.out.println("-".repeat(110));
        payments.forEach(p -> System.out.printf("%-6d %-15s %-22s %-24s %-14s %-11s %s%n", p.id(), p.receipt(),
                p.studentName(), p.concept(), InvoicePdfGenerator.formatMoney(p.amount()), p.date(), p.method()));
        BigDecimal total = payments.stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("\nTotal pagado: " + InvoicePdfGenerator.formatMoney(total) + " en " + payments.size() + " pagos");
    }

    public void printDelinquents() {
        List<Delinquent> list = controller.delinquents();
        if (list.isEmpty()) {
            System.out.println("\nNo hay estudiantes con pensiones pendientes por mas de un mes.");
            return;
        }
        System.out.printf("%nEstudiantes con pension pendiente por mas de un mes:%n");
        System.out.printf("%-5s %-26s %-26s %-12s %-12s %s%n", "ID", "Estudiante", "Acudiente", "Telefono",
                "Ultimo pago", "Dias");
        System.out.println("-".repeat(98));
        list.forEach(d -> System.out.printf("%-5d %-26s %-26s %-12s %-12s %s%n", d.studentId(), d.studentName(),
                d.guardianName() == null ? "-" : d.guardianName(),
                d.guardianPhone() == null ? "-" : d.guardianPhone(),
                d.lastPensionDate() == null ? "nunca" : d.lastPensionDate().toString(),
                d.lastPensionDate() == null ? "-" : ChronoUnit.DAYS.between(d.lastPensionDate(), java.time.LocalDate.now())));
        System.out.println("\nTotal morosos: " + list.size());
    }

    private void locateInvoice() {
        int paymentId = ConsoleInput.readInt("ID del pago: ", 1, Integer.MAX_VALUE);
        controller.findInvoice(paymentId).ifPresentOrElse(value -> {
            String[] parts = value.split("\\|", 2);
            System.out.println("\nFactura: " + parts[0]);
            System.out.println("Archivo: " + parts[1]);
        }, () -> System.out.println("\nEse pago no tiene factura generada por el sistema "
                + "(las facturas se generan al registrar un pago nuevo con la opcion 2)."));
    }
}
