package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicDao;
import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.NamedAmount;
import com.littlehorizons.erp.model.NamedCount;
import com.littlehorizons.erp.util.AppConfig;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.AppLogger;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Reports and statistics. Grade statistics are computed with lambdas and the Stream API. */
public class ReportController {

    /** One student whose average is below the support threshold. */
    public record AtRiskStudent(int studentId, String studentName, double average, long failingSubjects) {
    }

    private final ReportDao reportDao;
    private final AcademicDao academicDao;

    public ReportController(ReportDao reportDao, AcademicDao academicDao) {
        this.reportDao = reportDao;
        this.academicDao = academicDao;
    }

    public List<NamedCount> enrollmentByGrade(int year) {
        try {
            return reportDao.enrollmentByGrade(year);
        } catch (SQLException e) {
            AppLogger.error("Error building enrollment report", e);
            throw new AppException("No se pudo generar el reporte de matriculas.", e);
        }
    }

    public List<NamedAmount> incomeByConcept(int year) {
        try {
            return reportDao.incomeByConcept(year);
        } catch (SQLException e) {
            AppLogger.error("Error building income report", e);
            throw new AppException("No se pudo generar el reporte de ingresos.", e);
        }
    }

    public List<NamedCount> participationByEvent() {
        try {
            return reportDao.participationByEvent();
        } catch (SQLException e) {
            AppLogger.error("Error building event participation report", e);
            throw new AppException("No se pudo generar el reporte de participacion.", e);
        }
    }

    /** General course average of every subject for the year (key: "id - subject name"). */
    public Map<String, Double> courseAveragePerSubject(int year) {
        return loadRecords(year).stream()
                .filter(r -> !"Final".equals(r.period()))
                .collect(Collectors.groupingBy(r -> r.subjectId() + " - " + r.subjectName(),
                        java.util.TreeMap::new, Collectors.averagingDouble(AcademicRecord::value)));
    }

    /** Students whose year average is under the threshold, worst first: they need extra support. */
    public List<AtRiskStudent> studentsNeedingSupport(int year) {
        Map<Integer, List<AcademicRecord>> byStudent = loadRecords(year).stream()
                .filter(r -> !"Final".equals(r.period()))
                .collect(Collectors.groupingBy(AcademicRecord::studentId));

        return byStudent.values().stream()
                .map(records -> new AtRiskStudent(records.get(0).studentId(), records.get(0).studentName(),
                        records.stream().mapToDouble(AcademicRecord::value).average().orElse(0),
                        records.stream().collect(Collectors.groupingBy(AcademicRecord::subjectId,
                                Collectors.averagingDouble(AcademicRecord::value)))
                                .values().stream().filter(avg -> avg < AppConfig.SUPPORT_THRESHOLD).count()))
                .filter(s -> s.average() < AppConfig.SUPPORT_THRESHOLD)
                .sorted((a, b) -> Double.compare(a.average(), b.average()))
                .toList();
    }

    private List<AcademicRecord> loadRecords(int year) {
        try {
            return academicDao.findRecordsByYear(year);
        } catch (SQLException e) {
            AppLogger.error("Error loading academic records for reports", e);
            throw new AppException("No se pudo consultar las notas para el reporte.", e);
        }
    }
}
