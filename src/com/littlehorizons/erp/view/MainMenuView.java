package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.OperationCancelledException;

/** Main menu of the application. Each option opens the menu of one module. */
public class MainMenuView {

    private final StudentView studentView;
    private final AcademicView academicView;
    private final TeacherView teacherView;
    private final ExtracurricularView extracurricularView;
    private final FinanceView financeView;
    private final ResourceView resourceView;
    private final ReportView reportView;
    private final AttendanceImportView attendanceImportView;

    public MainMenuView(StudentView studentView, AcademicView academicView, TeacherView teacherView,
                        ExtracurricularView extracurricularView, FinanceView financeView,
                        ResourceView resourceView, ReportView reportView,
                        AttendanceImportView attendanceImportView) {
        this.studentView = studentView;
        this.academicView = academicView;
        this.teacherView = teacherView;
        this.extracurricularView = extracurricularView;
        this.financeView = financeView;
        this.resourceView = resourceView;
        this.reportView = reportView;
        this.attendanceImportView = attendanceImportView;
    }

    public void show() {
        boolean exit = false;
        while (!exit) {
            printBanner();
            System.out.println("1. Gestion de Estudiantes");
            System.out.println("2. Gestion Academica");
            System.out.println("3. Gestion de Profesores");
            System.out.println("4. Actividades Extracurriculares");
            System.out.println("5. Gestion Financiera");
            System.out.println("6. Gestion de Recursos");
            System.out.println("7. Reportes y Estadisticas");
            System.out.println("8. TEST - Importar Asistencias desde CSV");
            System.out.println("9. Salir");

            int option;
            try {
                option = ConsoleInput.readInt("\nSeleccione una opcion: ", 1, 9);
            } catch (OperationCancelledException e) {
                continue;
            }
            switch (option) {
                case 1 -> studentView.showMenu();
                case 2 -> academicView.showMenu();
                case 3 -> teacherView.showMenu();
                case 4 -> extracurricularView.showMenu();
                case 5 -> financeView.showMenu();
                case 6 -> resourceView.showMenu();
                case 7 -> reportView.showMenu();
                case 8 -> attendanceImportView.run();
                default -> exit = true;
            }
        }
        System.out.println("\nHasta pronto.");
    }

    private void printBanner() {
        System.out.println();
        System.out.println("+----------------------------------------------+");
        System.out.println("|   ERP Colegio Little Horizons                |");
        System.out.println("|   Sistema de Gestion Educativa               |");
        System.out.println("+----------------------------------------------+");
        System.out.println();
    }
}
