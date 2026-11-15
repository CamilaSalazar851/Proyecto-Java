package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.model.AbsenceRow;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.AppConfig;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.OperationCancelledException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Console screens for enrollment in subjects, grades and attendance. */
public class AcademicView {

    private static final List<String> ENROLL_PERIODS = List.of("I", "II", "III", "IV");
    private static final List<String> ATTENDANCE = List.of("Presente", "Ausente", "Tardanza", "Excusa Médica");

    private final AcademicController controller;

    public AcademicView(AcademicController controller) {
        this.controller = controller;
    }

    public void showMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== GESTION ACADEMICA ===");
            System.out.println("(escriba \'cancelar\' en cualquier campo para volver al menu)");
            System.out.println("1. Inscribir estudiante en una materia");
            System.out.println("2. Registrar nota");
            System.out.println("3. Historial academico y promedios de un estudiante");
            System.out.println("4. Promedios de una materia (curso)");
            System.out.println("5. Registrar asistencia");
            System.out.println("6. Reporte de inasistencias");
            System.out.println("7. Volver al menu principal");

            int option = ConsoleInput.readInt("Seleccione una opcion: ", 1, 7);
            try {
                switch (option) {
                    case 1 -> enroll();
                    case 2 -> recordGrade();
                    case 3 -> history();
                    case 4 -> subjectAverages();
                    case 5 -> attendance();
                    case 6 -> printAbsenceReport();
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

    private void enroll() {
        int studentId = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        List<Subject> subjects = controller.listSubjectsOfStudentGrade(studentId, AppConfig.CURRENT_YEAR);
        printSubjects(subjects);
        int subjectId = ConsoleInput.readInt("\nID de la materia: ", 1, Integer.MAX_VALUE);
        String period = ConsoleInput.readChoice("Periodo academico:", ENROLL_PERIODS);
        String section = ConsoleInput.readRequired("Seccion (A, B, C): ", 10).toUpperCase();
        controller.enrollInSubject(studentId, subjectId, AppConfig.CURRENT_YEAR, period, section);
        System.out.println("\n[OK] Inscripcion registrada.");
    }

    private void recordGrade() {
        int studentId = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        printSubjects(controller.listSubjectsOfStudentGrade(studentId, AppConfig.CURRENT_YEAR));
        int subjectId = ConsoleInput.readInt("\nID de la materia: ", 1, Integer.MAX_VALUE);
        int teacherId = ConsoleInput.readInt("ID del profesor que registra: ", 1, Integer.MAX_VALUE);
        String period = ConsoleInput.readChoice("Periodo:", AcademicRecord.PERIODS);
        double value = ConsoleInput.readGrade("Nota");
        controller.recordGrade(studentId, subjectId, teacherId, AppConfig.CURRENT_YEAR, period, value);
        System.out.printf(Locale.US, "%n[OK] Nota %.1f registrada.%n", value);
    }

    private void history() {
        int studentId = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        List<AcademicRecord> records = controller.studentHistory(studentId);
        if (records.isEmpty()) {
            System.out.println("\nEl estudiante aun no tiene notas registradas.");
            return;
        }
        System.out.println("\n--- Historial de " + records.get(0).studentName() + " ---");
        System.out.printf("%-6s %-8s %-30s %s%n", "Anio", "Periodo", "Materia", "Nota");
        records.forEach(r -> System.out.printf(Locale.US, "%-6d %-8s %-30s %.1f%n",
                r.year(), r.period(), r.subjectName(), r.value()));

        List<Integer> years = records.stream().map(AcademicRecord::year).distinct().sorted().toList();
        for (int year : years) {
            System.out.println("\nPromedios " + year + ":");
            Map<String, Double> perSubject = AcademicController.subjectAverages(records, year);
            perSubject.forEach((subject, avg) -> System.out.printf(Locale.US, "  %-30s %.2f%n", subject, avg));
            for (String period : ENROLL_PERIODS) {
                AcademicController.periodAverage(records, year, period).ifPresent(avg ->
                        System.out.printf(Locale.US, "  Promedio general periodo %-3s %.2f%n", period, avg));
            }
        }
    }

    private void subjectAverages() {
        int subjectId = ConsoleInput.readInt("ID de la materia: ", 1, Integer.MAX_VALUE);
        List<AcademicRecord> records = controller.subjectRecords(subjectId, AppConfig.CURRENT_YEAR);
        if (records.isEmpty()) {
            System.out.println("\nNo hay notas registradas para esa materia en " + AppConfig.CURRENT_YEAR + ".");
            return;
        }
        System.out.println("\nPromedio del curso en " + records.get(0).subjectName() + ":");
        AcademicController.courseAveragePerPeriod(records).forEach((period, avg) ->
                System.out.printf(Locale.US, "  Periodo %-6s %.2f%n", period, avg));
        double overall = records.stream().mapToDouble(AcademicRecord::value).average().orElse(0);
        long students = records.stream().map(AcademicRecord::studentId).distinct().count();
        System.out.printf(Locale.US, "  Promedio general: %.2f (%d estudiantes)%n", overall, students);
    }

    private void attendance() {
        int studentId = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        printSubjects(controller.listSubjectsOfStudentGrade(studentId, AppConfig.CURRENT_YEAR));
        int subjectId = ConsoleInput.readInt("\nID de la materia: ", 1, Integer.MAX_VALUE);
        int teacherId = ConsoleInput.readInt("ID del profesor que registra: ", 1, Integer.MAX_VALUE);
        var date = ConsoleInput.readDate("Fecha de la clase");
        String status = ConsoleInput.readChoice("Estado de asistencia:", ATTENDANCE);
        controller.recordAttendance(studentId, subjectId, teacherId, date, status);
        System.out.println("\n[OK] Asistencia registrada (" + status + ").");
    }

    public void printAbsenceReport() {
        int days = ConsoleInput.readInt("Revisar los ultimos cuantos dias (7-365): ", 7, 365);
        List<AbsenceRow> rows = controller.absenceReport(days);
        if (rows.isEmpty()) {
            System.out.println("\nNo hay estudiantes con mas de " + AppConfig.MAX_ABSENCES_PER_WEEK
                    + " ausencias en una misma semana.");
            return;
        }
        System.out.printf("%nEstudiantes con mas de %d ausencias en una semana:%n", AppConfig.MAX_ABSENCES_PER_WEEK);
        System.out.printf("%-5s %-32s %-10s %s%n", "ID", "Estudiante", "Semana", "Ausencias");
        rows.forEach(r -> System.out.printf("%-5d %-32s %-10s %d%n", r.studentId(), r.studentName(),
                r.week(), r.absences()));
        String names = rows.stream().map(AbsenceRow::studentName).distinct().collect(Collectors.joining(", "));
        System.out.println("\nRequieren seguimiento: " + names);
    }

    private void printSubjects(List<Subject> subjects) {
        System.out.printf("%nMaterias del grado:%n%-5s %-12s %-30s %s%n", "ID", "Codigo", "Materia", "Horas/sem");
        subjects.forEach(s -> System.out.printf("%-5d %-12s %-30s %d%n", s.id(), s.code(), s.name(), s.weeklyHours()));
    }
}
