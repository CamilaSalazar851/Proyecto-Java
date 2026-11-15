package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.AcademicDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.ExtracurricularDao;
import com.littlehorizons.erp.dao.FinanceDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.view.AcademicView;
import com.littlehorizons.erp.view.ExtracurricularView;
import com.littlehorizons.erp.view.FinanceView;
import com.littlehorizons.erp.view.MainMenuView;
import com.littlehorizons.erp.view.ReportView;
import com.littlehorizons.erp.view.ResourceView;
import com.littlehorizons.erp.view.StudentView;
import com.littlehorizons.erp.view.TeacherView;

/**
 * Application entry point. It is the composition root: the only place where DAOs, controllers
 * and views are created and wired together (dependency injection by constructor).
 */
public class Main {

    public static void main(String[] args) {
        AppLogger.info("Application started");

        // DAOs (data layer)
        StudentDao studentDao = new StudentDao();
        GuardianDao guardianDao = new GuardianDao();
        EnrollmentDao enrollmentDao = new EnrollmentDao();
        SubjectDao subjectDao = new SubjectDao();
        TeacherDao teacherDao = new TeacherDao();
        AcademicDao academicDao = new AcademicDao();
        ExtracurricularDao extracurricularDao = new ExtracurricularDao();
        FinanceDao financeDao = new FinanceDao();
        ResourceDao resourceDao = new ResourceDao();
        ReportDao reportDao = new ReportDao();

        // Controllers (business logic)
        StudentController studentController = new StudentController(studentDao, guardianDao, enrollmentDao);
        TeacherController teacherController = new TeacherController(teacherDao, subjectDao);
        AcademicController academicController =
                new AcademicController(academicDao, studentDao, subjectDao, teacherDao, enrollmentDao);
        ExtracurricularController extracurricularController =
                new ExtracurricularController(extracurricularDao, studentDao, teacherDao);
        FinanceController financeController = new FinanceController(financeDao, studentDao, guardianDao);
        ResourceController resourceController = new ResourceController(resourceDao, studentDao, teacherDao);
        ReportController reportController = new ReportController(reportDao, academicDao);

        // Views (console)
        StudentView studentView = new StudentView(studentController);
        AcademicView academicView = new AcademicView(academicController);
        TeacherView teacherView = new TeacherView(teacherController);
        ExtracurricularView extracurricularView = new ExtracurricularView(extracurricularController);
        FinanceView financeView = new FinanceView(financeController);
        ResourceView resourceView = new ResourceView(resourceController);
        ReportView reportView = new ReportView(reportController, extracurricularController,
                academicView, financeView, resourceView);

        new MainMenuView(studentView, academicView, teacherView, extracurricularView, financeView,
                resourceView, reportView).show();

        AppLogger.info("Application closed");
    }
}
