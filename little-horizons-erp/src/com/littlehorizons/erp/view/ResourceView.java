package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.model.Category;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.OperationCancelledException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Console screens for the resource inventory and loans. */
public class ResourceView {

    private static final List<String> STATUS_FILTER =
            List.of("Todos", "Disponible", "Prestado", "En Mantenimiento", "Dado de Baja");
    private static final List<String> SETTABLE_STATUS = List.of("Disponible", "En Mantenimiento", "Dado de Baja");
    private static final List<String> BORROWERS = List.of("Estudiante", "Profesor");

    private final ResourceController controller;

    public ResourceView(ResourceController controller) {
        this.controller = controller;
    }

    public void showMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== GESTION DE RECURSOS ===");
            System.out.println("(escriba \'cancelar\' en cualquier campo para volver al menu)");
            System.out.println("1. Ver inventario de recursos");
            System.out.println("2. Registrar recurso");
            System.out.println("3. Prestar recurso");
            System.out.println("4. Registrar devolucion");
            System.out.println("5. Ver prestamos activos");
            System.out.println("6. Reporte de prestamos vencidos");
            System.out.println("7. Cambiar estado de un recurso (mantenimiento / baja)");
            System.out.println("8. Editar datos de un recurso");
            System.out.println("9. Volver al menu principal");

            int option = ConsoleInput.readInt("Seleccione una opcion: ", 1, 9);
            try {
                switch (option) {
                    case 1 -> printInventory();
                    case 2 -> register();
                    case 3 -> lend();
                    case 4 -> giveBack();
                    case 5 -> printLoans(controller.openLoans(), "Prestamos activos");
                    case 6 -> printOverdue();
                    case 7 -> changeStatus();
                    case 8 -> edit();
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

    private void printInventory() {
        String filter = ConsoleInput.readChoice("Filtrar por estado:", STATUS_FILTER);
        List<Resource> resources = controller.listResources("Todos".equals(filter) ? null : filter);
        System.out.printf("%n%-5s %-10s %-36s %-24s %-22s %s%n", "ID", "Codigo", "Recurso", "Categoria",
                "Ubicacion", "Estado");
        System.out.println("-".repeat(115));
        resources.forEach(r -> System.out.printf("%-5d %-10s %-36s %-24s %-22s %s%n", r.id(), r.code(), r.name(),
                r.category(), r.location() == null ? "-" : r.location(), r.status()));
        System.out.println("\nTotal: " + resources.size());
    }

    private void register() {
        String code = ConsoleInput.readRequired("Codigo unico (ej: LIB-041): ", 50).toUpperCase();
        String name = ConsoleInput.readRequired("Nombre del recurso: ", 150);
        List<Category> categories = controller.listCategories();
        String chosen = ConsoleInput.readChoice("Categoria:", categories.stream().map(Category::name).toList());
        Category category = categories.stream().filter(c -> c.name().equals(chosen)).findFirst().orElseThrow();
        String description = ConsoleInput.readOptional("Descripcion", 500);
        String location = ConsoleInput.readRequired("Ubicacion fisica: ", 100);
        LocalDate acquired = ConsoleInput.readDate("Fecha de adquisicion");
        System.out.println("\n[OK] Recurso registrado con ID "
                + controller.registerResource(code, name, category.id(), description, location, acquired) + ".");
    }

    private void lend() {
        String type = ConsoleInput.readChoice("Quien solicita el recurso:", BORROWERS);
        int borrowerId = ConsoleInput.readInt("ID del " + type.toLowerCase() + ": ", 1, Integer.MAX_VALUE);
        System.out.println("\nRecursos disponibles:");
        controller.listResources("Disponible").forEach(r -> System.out.printf("  %-5d %-10s %s%n", r.id(), r.code(), r.name()));
        int resourceId = ConsoleInput.readInt("\nID del recurso: ", 1, Integer.MAX_VALUE);
        int days = ConsoleInput.readInt("Dias de prestamo (1-30): ", 1, 30);
        controller.lendResource(type, borrowerId, resourceId, days);
        System.out.println("\n[OK] Prestamo registrado. Devolucion programada: " + LocalDate.now().plusDays(days));
    }

    private void giveBack() {
        printLoans(controller.openLoans(), "Prestamos pendientes de devolucion");
        String type = ConsoleInput.readChoice("\nTipo de prestatario del prestamo:", BORROWERS);
        int loanId = ConsoleInput.readInt("ID del prestamo: ", 1, Integer.MAX_VALUE);
        controller.returnResource(type, loanId);
        System.out.println("\n[OK] Devolucion registrada. El recurso quedo disponible.");
    }

    private void edit() {
        int id = ConsoleInput.readInt("ID del recurso a editar: ", 1, Integer.MAX_VALUE);
        System.out.println("(deje en blanco lo que no quiera cambiar)");
        String name = ConsoleInput.readOptional("Nuevo nombre", 150);
        String location = ConsoleInput.readOptional("Nueva ubicacion fisica", 100);
        String description = ConsoleInput.readOptional("Nueva descripcion", 500);
        controller.updateResource(id, name, location, description);
        System.out.println("\n[OK] Recurso actualizado.");
    }

    private void changeStatus() {
        int resourceId = ConsoleInput.readInt("ID del recurso: ", 1, Integer.MAX_VALUE);
        String status = ConsoleInput.readChoice("Nuevo estado:", SETTABLE_STATUS);
        controller.changeResourceStatus(resourceId, status);
        System.out.println("\n[OK] Estado actualizado a " + status + ".");
    }

    public void printOverdue() {
        printLoans(controller.overdueLoans(), "Prestamos vencidos (no devueltos en la fecha programada)");
    }

    private void printLoans(List<Loan> loans, String title) {
        System.out.println("\n" + title + ":");
        if (loans.isEmpty()) {
            System.out.println("  No hay registros.");
            return;
        }
        System.out.printf("%-5s %-11s %-10s %-30s %-24s %-11s %-11s %s%n", "ID", "Tipo", "Codigo", "Recurso",
                "Prestatario", "Prestamo", "Vence", "Estado");
        System.out.println("-".repeat(122));
        loans.forEach(l -> System.out.printf("%-5d %-11s %-10s %-30s %-24s %-11s %-11s %s%n", l.id(),
                l.borrowerType(), l.resourceCode(), l.resourceName(), l.borrowerName(), l.loanDate(), l.dueDate(),
                l.returnDate() == null && l.dueDate().isBefore(LocalDate.now())
                        ? l.status() + " (" + ChronoUnit.DAYS.between(l.dueDate(), LocalDate.now()) + " dias)"
                        : l.status()));
        System.out.println("\nTotal: " + loans.size());
    }
}
