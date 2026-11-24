package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.model.NamedAmount;
import com.littlehorizons.erp.model.NamedCount;
import com.littlehorizons.erp.util.AppConfig;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.OperationCancelledException;
import com.littlehorizons.erp.util.InvoicePdfGenerator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** Console screens for reports and statistics. */
public class ReportView {

    private final ReportController reports;
    private final ExtracurricularController extracurricular;
    private final AcademicView academicView;
    private final FinanceView financeView;
    private final ResourceView resourceView;

    public ReportView(ReportController reports, ExtracurricularController extracurricular,
                      AcademicView academicView, FinanceView financeView, ResourceView resourceView) {
        this.reports = reports;
        this.extracurricular = extracurricular;
        this.academicView = academicView;
        this.financeView = financeView;
        this.resourceView = resourceView;
    }

    public void showMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== REPORTES Y ESTADISTICAS ===");
            System.out.println("(escriba \'cancelar\' en cualquier campo para volver al menu)");
            System.out.println("1. Estudiantes matriculados por grado");
            System.out.println("2. Promedio del curso por materia");
            System.out.println("3. Estudiantes que necesitan apoyo academico");
            System.out.println("4. Reporte de inasistencias");
            System.out.println("5. Ingresos por concepto de pago");
            System.out.println("6. Reporte de morosos");
            System.out.println("7. Participacion por evento");
            System.out.println("8. Ocupacion de clubes");
            System.out.println("9. Prestamos vencidos");
            System.out.println("10. Volver al menu principal");

            int option = ConsoleInput.readInt("Seleccione una opcion: ", 1, 10);
            try {
                switch (option) {
                    case 1 -> enrollment();
                    case 2 -> courseAverages();
                    case 3 -> support();
                    case 4 -> academicView.printAbsenceReport();
                    case 5 -> income();
                    case 6 -> financeView.printDelinquents();
                    case 7 -> participation();
                    case 8 -> clubOccupancy();
                    case 9 -> resourceView.printOverdue();
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

    private void enrollment() {
        List<NamedCount> rows = reports.enrollmentByGrade(AppConfig.CURRENT_YEAR);
        System.out.println("\nEstudiantes con matricula activa en " + AppConfig.CURRENT_YEAR + ":");
        rows.forEach(r -> System.out.printf("  %-14s %3d  %s%n", r.name(), r.count(), "#".repeat((int) r.count())));
        System.out.println("  Total: " + rows.stream().mapToLong(NamedCount::count).sum());
    }

    private void courseAverages() {
        var averages = reports.courseAveragePerSubject(AppConfig.CURRENT_YEAR);
        if (averages.isEmpty()) {
            System.out.println("\nNo hay notas registradas en " + AppConfig.CURRENT_YEAR + ".");
            return;
        }
        System.out.println("\nPromedio del curso por materia (" + AppConfig.CURRENT_YEAR + "):");
        averages.forEach((subject, avg) -> System.out.printf(Locale.US, "  %-36s %.2f%n", subject, avg));
    }

    private void support() {
        var students = reports.studentsNeedingSupport(AppConfig.CURRENT_YEAR);
        if (students.isEmpty()) {
            System.out.printf(Locale.US, "%nNingun estudiante tiene promedio inferior a %.1f.%n", AppConfig.SUPPORT_THRESHOLD);
            return;
        }
        System.out.printf(Locale.US, "%nEstudiantes con promedio inferior a %.1f:%n", AppConfig.SUPPORT_THRESHOLD);
        System.out.printf("%-5s %-32s %-10s %s%n", "ID", "Estudiante", "Promedio", "Materias en riesgo");
        students.forEach(s -> System.out.printf(Locale.US, "%-5d %-32s %-10.2f %d%n", s.studentId(),
                s.studentName(), s.average(), s.failingSubjects()));
    }

    private void income() {
        List<NamedAmount> rows = reports.incomeByConcept(AppConfig.CURRENT_YEAR);
        System.out.println("\nIngresos " + AppConfig.CURRENT_YEAR + " por concepto:");
        rows.forEach(r -> System.out.printf("  %-28s %-18s (%d pagos)%n", r.name(),
                InvoicePdfGenerator.formatMoney(r.total()), r.count()));
        BigDecimal total = rows.stream().map(NamedAmount::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("  TOTAL: " + InvoicePdfGenerator.formatMoney(total));
    }

    private void participation() {
        System.out.println("\nParticipantes por evento:");
        reports.participationByEvent().forEach(r -> System.out.printf("  %-32s %3d%n", r.name(), r.count()));
    }

    private void clubOccupancy() {
        System.out.println("\nOcupacion de clubes:");
        extracurricular.listClubs().stream()
                .sorted((a, b) -> Integer.compare(b.activeMembers(), a.activeMembers()))
                .forEach(c -> System.out.printf("  %-34s %2d/%-3d %s%n", c.name(), c.activeMembers(), c.capacity(),
                        c.isFull() ? "LLENO" : ""));
    }
}
