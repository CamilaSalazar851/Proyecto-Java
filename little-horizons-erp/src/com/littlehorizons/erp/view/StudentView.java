package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.AppConfig;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.OperationCancelledException;

import java.util.List;
import java.util.Optional;

/** Console screens for the student module. */
public class StudentView {

    private static final List<String> RELATIONSHIPS =
            List.of("Padre", "Madre", "Abuelo", "Abuela", "Tío", "Tía", "Tutor Legal");
    private static final List<String> GUARDIAN_DOCS = List.of("CC", "CE", "Pasaporte");

    private final StudentController controller;

    public StudentView(StudentController controller) {
        this.controller = controller;
    }

    public void showMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== GESTION DE ESTUDIANTES ===");
            System.out.println("(escriba \'cancelar\' en cualquier campo para volver al menu)");
            System.out.println("1. Registrar estudiante (con su acudiente)");
            System.out.println("2. Listar estudiantes");
            System.out.println("3. Buscar estudiante por ID");
            System.out.println("4. Cambiar estado de un estudiante");
            System.out.println("5. Matricular estudiante en un grado");
            System.out.println("6. Vincular acudiente adicional");
            System.out.println("7. Editar datos de un estudiante");
            System.out.println("8. Retirar matricula del anio");
            System.out.println("9. Volver al menu principal");

            int option = ConsoleInput.readInt("Seleccione una opcion: ", 1, 9);
            try {
                switch (option) {
                    case 1 -> register();
                    case 2 -> list();
                    case 3 -> search();
                    case 4 -> changeStatus();
                    case 5 -> enroll();
                    case 6 -> linkGuardian();
                    case 7 -> edit();
                    case 8 -> withdrawEnrollment();
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
        System.out.println("\n--- Registro de estudiante ---");
        Student s = new Student();
        s.setDocumentType(ConsoleInput.readChoice("Tipo de documento:", Student.DOCUMENT_TYPES));
        s.setDocumentNumber(ConsoleInput.readRequired("Numero de documento: ", 20));
        s.setFirstName(ConsoleInput.readRequired("Primer nombre: ", 100));
        s.setMiddleName(ConsoleInput.readOptional("Segundo nombre", 100));
        s.setFirstLastName(ConsoleInput.readRequired("Primer apellido: ", 100));
        s.setSecondLastName(ConsoleInput.readOptional("Segundo apellido", 100));
        s.setBirthDate(ConsoleInput.readDate("Fecha de nacimiento"));
        s.setGender(ConsoleInput.readChoice("Genero:", Student.GENDERS));
        s.setAddress(ConsoleInput.readOptional("Direccion", 255));
        s.setPhone(ConsoleInput.readOptional("Telefono", 20));
        s.setEmail(ConsoleInput.readOptional("Correo electronico", 100));
        s.setAdmissionDate(ConsoleInput.readDate("Fecha de ingreso"));
        s.setStatus("Activo");

        System.out.println("\nTodo estudiante debe tener al menos un acudiente.");
        Guardian guardian = askGuardian();

        int id = controller.registerStudent(s, guardian);
        System.out.println("\n[OK] Estudiante registrado con ID " + id + ".");
        if (ConsoleInput.confirm("Desea matricularlo ahora en un grado para " + AppConfig.CURRENT_YEAR + "?")) {
            enrollStudent(id);
        }
    }

    /** Looks the guardian up by document; if it does not exist, asks for the full data. */
    private Guardian askGuardian() {
        String document = ConsoleInput.readRequired("Numero de documento del acudiente: ", 20);
        Optional<Guardian> existing = controller.findGuardianByDocument(document);
        if (existing.isPresent()) {
            Guardian g = existing.get();
            System.out.println("Acudiente encontrado: " + g.fullName() + " (" + g.relationship() + ")");
            if (ConsoleInput.confirm("Usar este acudiente?")) {
                return g;
            }
            throw new AppException("Registro cancelado. Verifique el documento del acudiente.");
        }
        String docType = ConsoleInput.readChoice("Tipo de documento del acudiente:", GUARDIAN_DOCS);
        String firstName = ConsoleInput.readRequired("Primer nombre del acudiente: ", 100);
        String firstLast = ConsoleInput.readRequired("Primer apellido del acudiente: ", 100);
        String secondLast = ConsoleInput.readOptional("Segundo apellido del acudiente", 100);
        String relationship = ConsoleInput.readChoice("Parentesco:", RELATIONSHIPS);
        String phone = ConsoleInput.readRequired("Telefono principal: ", 20);
        String email = ConsoleInput.readOptional("Correo electronico del acudiente", 100);
        String address = ConsoleInput.readOptional("Direccion del acudiente", 255);
        return new Guardian(0, docType, document, firstName, firstLast, secondLast, relationship,
                phone, email, address);
    }

    private void list() {
        List<Student> students = controller.listStudents();
        if (students.isEmpty()) {
            System.out.println("\nNo hay estudiantes registrados.");
            return;
        }
        System.out.printf("%n%-5s %-12s %-38s %-10s%n", "ID", "Documento", "Nombre", "Estado");
        System.out.println("-".repeat(68));
        students.forEach(s -> System.out.printf("%-5d %-12s %-38s %-10s%n",
                s.getId(), s.getDocumentNumber(), s.getFullName(), s.getStatus()));
        System.out.println("\nTotal: " + students.size());
    }

    private void search() {
        int id = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        controller.findById(id).ifPresentOrElse(s -> {
            System.out.println("\n--- Datos del estudiante ---");
            System.out.println("ID:               " + s.getId());
            System.out.println("Documento:        " + s.getDocumentType() + " " + s.getDocumentNumber());
            System.out.println("Nombre:           " + s.getFullName());
            System.out.println("Nacimiento:       " + s.getBirthDate());
            System.out.println("Genero:           " + s.getGender());
            System.out.println("Direccion:        " + orDash(s.getAddress()));
            System.out.println("Telefono:         " + orDash(s.getPhone()));
            System.out.println("Correo:           " + orDash(s.getEmail()));
            System.out.println("Fecha de ingreso: " + s.getAdmissionDate());
            System.out.println("Estado:           " + s.getStatus());
            System.out.println("Acudientes:");
            controller.listGuardians(id).forEach(g -> System.out.printf("  - %s (%s) Tel: %s%n",
                    g.fullName(), g.relationship(), g.phone()));
        }, () -> System.out.println("\n[!] No existe un estudiante con ID " + id + "."));
    }

    private void changeStatus() {
        int id = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        if (controller.findById(id).isEmpty()) {
            System.out.println("\n[!] No existe un estudiante con ID " + id + ".");
            return;
        }
        String status = ConsoleInput.readChoice("Nuevo estado:", Student.STATUSES);
        if (ConsoleInput.confirm("Confirma el cambio a '" + status + "'?")) {
            controller.changeStatus(id, status);
            System.out.println("\n[OK] Estado actualizado.");
        } else {
            System.out.println("\nOperacion cancelada.");
        }
    }

    private void enroll() {
        int id = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        if (controller.findById(id).isEmpty()) {
            System.out.println("\n[!] No existe un estudiante con ID " + id + ".");
            return;
        }
        enrollStudent(id);
    }

    private void enrollStudent(int studentId) {
        List<Grade> grades = controller.listGrades();
        String chosen = ConsoleInput.readChoice("Grado para " + AppConfig.CURRENT_YEAR + ":",
                grades.stream().map(Grade::name).toList());
        Grade grade = grades.stream().filter(g -> g.name().equals(chosen)).findFirst().orElseThrow();
        controller.enrollInGrade(studentId, grade.id(), AppConfig.CURRENT_YEAR);
        System.out.println("\n[OK] Matricula registrada en " + grade.name() + ".");
    }

    private void linkGuardian() {
        int id = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        if (controller.findById(id).isEmpty()) {
            System.out.println("\n[!] No existe un estudiante con ID " + id + ".");
            return;
        }
        controller.linkGuardian(id, askGuardian());
        System.out.println("\n[OK] Acudiente vinculado.");
    }

    private void edit() {
        int id = ConsoleInput.readInt("ID del estudiante a editar: ", 1, Integer.MAX_VALUE);
        Student s = controller.findById(id).orElse(null);
        if (s == null) {
            System.out.println("\n[!] No existe un estudiante con ID " + id + ".");
            return;
        }
        System.out.println("\nEditando a " + s.getFullName() + " (documento y fecha de nacimiento no se editan).");
        s.setFirstName(ConsoleInput.readKeep("Primer nombre", s.getFirstName(), 100));
        s.setMiddleName(ConsoleInput.readKeep("Segundo nombre", s.getMiddleName(), 100));
        s.setFirstLastName(ConsoleInput.readKeep("Primer apellido", s.getFirstLastName(), 100));
        s.setSecondLastName(ConsoleInput.readKeep("Segundo apellido", s.getSecondLastName(), 100));
        s.setAddress(ConsoleInput.readKeep("Direccion", s.getAddress(), 255));
        s.setPhone(ConsoleInput.readKeep("Telefono", s.getPhone(), 20));
        s.setEmail(ConsoleInput.readKeep("Correo electronico", s.getEmail(), 100));
        controller.updateStudent(s);
        System.out.println("\n[OK] Datos actualizados.");
    }

    private void withdrawEnrollment() {
        int id = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        if (ConsoleInput.confirm("Retirar la matricula de " + AppConfig.CURRENT_YEAR + " del estudiante " + id + "?")) {
            controller.withdrawEnrollment(id, AppConfig.CURRENT_YEAR);
            System.out.println("\n[OK] Matricula retirada.");
        } else {
            System.out.println("\nOperacion cancelada.");
        }
    }

    private static String orDash(String value) {
        return value == null ? "-" : value;
    }
}
