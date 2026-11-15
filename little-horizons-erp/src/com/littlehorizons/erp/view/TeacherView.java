package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.AppConfig;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.OperationCancelledException;

import java.time.LocalTime;
import java.util.List;

/** Console screens for the teacher module. */
public class TeacherView {

    private static final List<String> DAYS = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes");

    private final TeacherController controller;

    public TeacherView(TeacherController controller) {
        this.controller = controller;
    }

    public void showMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== GESTION DE PROFESORES ===");
            System.out.println("(escriba \'cancelar\' en cualquier campo para volver al menu)");
            System.out.println("1. Registrar profesor");
            System.out.println("2. Listar profesores");
            System.out.println("3. Asignar materia a un profesor");
            System.out.println("4. Registrar horario de una asignacion");
            System.out.println("5. Ver carga academica de un profesor");
            System.out.println("6. Ver todas las asignaciones del anio");
            System.out.println("7. Editar datos de un profesor");
            System.out.println("8. Activar / desactivar profesor");
            System.out.println("9. Volver al menu principal");

            int option = ConsoleInput.readInt("Seleccione una opcion: ", 1, 9);
            try {
                switch (option) {
                    case 1 -> register();
                    case 2 -> list();
                    case 3 -> assign();
                    case 4 -> addSchedule();
                    case 5 -> workload();
                    case 6 -> printAssignments(controller.listAssignments(AppConfig.CURRENT_YEAR));
                    case 7 -> edit();
                    case 8 -> changeStatus();
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

    private void register() {
        System.out.println("\n--- Registro de profesor ---");
        String docType = ConsoleInput.readChoice("Tipo de documento:", Teacher.DOCUMENT_TYPES);
        String document = ConsoleInput.readRequired("Numero de documento: ", 20);
        String firstName = ConsoleInput.readRequired("Primer nombre: ", 100);
        String firstLast = ConsoleInput.readRequired("Primer apellido: ", 100);
        String secondLast = ConsoleInput.readOptional("Segundo apellido", 100);
        String email = ConsoleInput.readRequired("Correo electronico: ", 100);
        String phone = ConsoleInput.readOptional("Telefono", 20);
        String specialization = ConsoleInput.readOptional("Especializacion", 100);
        var hireDate = ConsoleInput.readDate("Fecha de contratacion");

        Teacher teacher = new Teacher(0, docType, document, firstName, firstLast, secondLast, email,
                phone, specialization, hireDate, "Activo");
        System.out.println("\n[OK] Profesor registrado con ID " + controller.registerTeacher(teacher) + ".");
    }

    private void list() {
        List<Teacher> teachers = controller.listTeachers();
        System.out.printf("%n%-5s %-12s %-32s %-24s %-9s%n", "ID", "Documento", "Nombre", "Especializacion", "Estado");
        System.out.println("-".repeat(86));
        teachers.forEach(t -> System.out.printf("%-5d %-12s %-32s %-24s %-9s%n", t.id(), t.documentNumber(),
                t.fullName(), t.specialization() == null ? "-" : t.specialization(), t.status()));
        System.out.println("\nTotal: " + teachers.size());
    }

    private void assign() {
        int teacherId = ConsoleInput.readInt("ID del profesor: ", 1, Integer.MAX_VALUE);
        List<Subject> subjects = controller.listSubjects();
        System.out.printf("%nMaterias disponibles:%n%-5s %-12s %-30s %-12s%n", "ID", "Codigo", "Materia", "Grado");
        subjects.forEach(s -> System.out.printf("%-5d %-12s %-30s %-12s%n", s.id(), s.code(), s.name(), s.gradeName()));
        int subjectId = ConsoleInput.readInt("\nID de la materia: ", 1, Integer.MAX_VALUE);
        String section = ConsoleInput.readRequired("Seccion o grupo (A, B, C): ", 10);

        int id = controller.assignSubject(teacherId, subjectId, AppConfig.CURRENT_YEAR, section.toUpperCase());
        System.out.println("\n[OK] Asignacion creada con ID " + id + ". Ahora puede registrar su horario (opcion 4).");
    }

    private void addSchedule() {
        int assignmentId = ConsoleInput.readInt("ID de la asignacion: ", 1, Integer.MAX_VALUE);
        String day = ConsoleInput.readChoice("Dia de la semana:", DAYS);
        LocalTime start = ConsoleInput.readTime("Hora de inicio");
        LocalTime end = ConsoleInput.readTime("Hora de fin");
        controller.addSchedule(assignmentId, day, start, end);
        System.out.println("\n[OK] Horario registrado sin conflictos.");
    }

    private void workload() {
        int teacherId = ConsoleInput.readInt("ID del profesor: ", 1, Integer.MAX_VALUE);
        controller.findTeacher(teacherId).ifPresentOrElse(
                t -> {
                    System.out.println("\nCarga academica de " + t.fullName() + " (" + AppConfig.CURRENT_YEAR + ")");
                    printAssignments(controller.listAssignmentsByTeacher(teacherId, AppConfig.CURRENT_YEAR));
                },
                () -> System.out.println("\n[!] No existe un profesor con ID " + teacherId + "."));
    }

    private void edit() {
        int id = ConsoleInput.readInt("ID del profesor a editar: ", 1, Integer.MAX_VALUE);
        Teacher t = controller.findTeacher(id).orElse(null);
        if (t == null) {
            System.out.println("\n[!] No existe un profesor con ID " + id + ".");
            return;
        }
        System.out.println("\nEditando a " + t.fullName() + " (documento y nombre no se editan).");
        String email = ConsoleInput.readKeep("Correo electronico", t.email(), 100);
        String phone = ConsoleInput.readKeep("Telefono", t.phone(), 20);
        String specialization = ConsoleInput.readKeep("Especializacion", t.specialization(), 100);
        controller.updateTeacher(id, email, phone, specialization);
        System.out.println("\n[OK] Datos actualizados.");
    }

    private void changeStatus() {
        int id = ConsoleInput.readInt("ID del profesor: ", 1, Integer.MAX_VALUE);
        if (controller.findTeacher(id).isEmpty()) {
            System.out.println("\n[!] No existe un profesor con ID " + id + ".");
            return;
        }
        String status = ConsoleInput.readChoice("Nuevo estado:", List.of("Activo", "Inactivo", "Licencia"));
        controller.changeTeacherStatus(id, status);
        System.out.println("\n[OK] Estado actualizado a " + status + ".");
    }

    private void printAssignments(List<Assignment> assignments) {
        if (assignments.isEmpty()) {
            System.out.println("\nNo hay asignaciones registradas.");
            return;
        }
        System.out.printf("%n%-5s %-22s %-34s %-4s %s%n", "ID", "Profesor", "Materia", "Sec", "Horario");
        System.out.println("-".repeat(100));
        assignments.forEach(a -> System.out.printf("%-5d %-22s %-34s %-4s %s%n", a.id(), a.teacherName(),
                a.subjectName(), a.section(), a.schedule() == null ? "(sin horario)" : a.schedule()));
    }
}
