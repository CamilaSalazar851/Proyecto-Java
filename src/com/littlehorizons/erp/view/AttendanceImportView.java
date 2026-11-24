package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AttendanceImportController;
import com.littlehorizons.erp.model.AttendanceImportResult;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.ConsoleInput;

/** Console screen of the TEST option that imports attendance from asistencias_importar.csv. */
public class AttendanceImportView {

    private final AttendanceImportController controller;

    public AttendanceImportView(AttendanceImportController controller) {
        this.controller = controller;
    }

    public void run() {
        System.out.println("\n=== TEST - Importar Asistencias desde CSV ===");
        try {
            printReport(controller.importFromCsv());
            ConsoleInput.waitForEnter();
        } catch (AppException e) {
            System.out.println(e.getMessage());
        }
    }

    private void printReport(AttendanceImportResult result) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║           REPORTE DE IMPORTACIÓN DE ASISTENCIAS                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Total de líneas en el archivo: " + result.total());
        System.out.println("Líneas procesadas exitosamente: " + result.success());
        System.out.println("Líneas con errores (no procesadas): " + result.failed());
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("Presione Enter para volver al menú principal...");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}
