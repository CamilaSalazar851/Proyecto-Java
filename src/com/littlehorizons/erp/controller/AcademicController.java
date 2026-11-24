package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.AbsenceRow;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.AppConfig;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.AppLogger;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Business rules for subject enrollment, grades, averages and attendance. */
public class AcademicController {

    private final AcademicDao academicDao;
    private final StudentDao studentDao;
    private final SubjectDao subjectDao;
    private final TeacherDao teacherDao;
    private final EnrollmentDao enrollmentDao;

    public AcademicController(AcademicDao academicDao, StudentDao studentDao, SubjectDao subjectDao,
                              TeacherDao teacherDao, EnrollmentDao enrollmentDao) {
        this.academicDao = academicDao;
        this.studentDao = studentDao;
        this.subjectDao = subjectDao;
        this.teacherDao = teacherDao;
        this.enrollmentDao = enrollmentDao;
    }

    // ---------- enrollment ----------

    public List<Subject> listSubjectsOfStudentGrade(int studentId, int year) {
        try {
            int gradeId = enrollmentDao.findActiveGradeId(studentId, year).orElseThrow(() ->
                    new AppException("El estudiante no tiene matricula activa en " + year + "."));
            return subjectDao.findByGrade(gradeId);
        } catch (SQLException e) {
            AppLogger.error("Error listing subjects for student " + studentId, e);
            throw new AppException("No se pudo consultar las materias del grado.", e);
        }
    }

    public void enrollInSubject(int studentId, int subjectId, int year, String period, String section) {
        try {
            requireActiveStudent(studentId);
            Subject subject = requireSubject(subjectId);
            int gradeId = enrollmentDao.findActiveGradeId(studentId, year).orElseThrow(() ->
                    new AppException("El estudiante no tiene matricula activa en " + year + "."));
            if (gradeId != subject.gradeId()) {
                throw new AppException("La materia pertenece a " + subject.gradeName()
                        + " y no al grado del estudiante.");
            }
            if (academicDao.isEnrolled(studentId, subjectId, year, period)) {
                throw new AppException("El estudiante ya esta inscrito en esa materia para el periodo " + period + ".");
            }
            academicDao.enroll(studentId, subjectId, year, period, section);
        } catch (SQLException e) {
            AppLogger.error("Error enrolling student " + studentId + " in subject " + subjectId, e);
            throw new AppException("No se pudo inscribir al estudiante en la materia.", e);
        }
    }

    // ---------- grades ----------

    public void recordGrade(int studentId, int subjectId, int teacherId, int year, String period, double value) {
        try {
            requireActiveStudent(studentId);
            requireSubject(subjectId);
            if (!teacherDao.isTeacherAssigned(teacherId, subjectId, year)) {
                throw new AppException("El profesor no tiene asignada esa materia en " + year + ".");
            }
            boolean enrolled = "Final".equals(period)
                    ? academicDao.isEnrolledAnyPeriod(studentId, subjectId, year)
                    : academicDao.isEnrolled(studentId, subjectId, year, period);
            if (!enrolled) {
                throw new AppException("El estudiante no esta inscrito en esa materia para ese periodo.");
            }
            academicDao.saveGrade(studentId, subjectId, teacherId, year, period, value);
        } catch (SQLException e) {
            AppLogger.error("Error saving grade for student " + studentId, e);
            throw new AppException("No se pudo registrar la nota.", e);
        }
    }

    /** Full grade history of a student since enrollment. */
    public List<AcademicRecord> studentHistory(int studentId) {
        try {
            requireStudent(studentId);
            return academicDao.findRecordsByStudent(studentId);
        } catch (SQLException e) {
            AppLogger.error("Error reading history of student " + studentId, e);
            throw new AppException("No se pudo consultar el historial academico.", e);
        }
    }

    public List<AcademicRecord> subjectRecords(int subjectId, int year) {
        try {
            return academicDao.findRecordsBySubject(subjectId, year);
        } catch (SQLException e) {
            AppLogger.error("Error reading records of subject " + subjectId, e);
            throw new AppException("No se pudo consultar las notas de la materia.", e);
        }
    }

    // ---------- automatic averages (Streams) ----------

    /** Average of a student per subject in a year (the "Final" period is excluded). */
    public static Map<String, Double> subjectAverages(List<AcademicRecord> records, int year) {
        return records.stream()
                .filter(r -> r.year() == year && !"Final".equals(r.period()))
                .collect(Collectors.groupingBy(AcademicRecord::subjectName, TreeMap::new,
                        Collectors.averagingDouble(AcademicRecord::value)));
    }

    /** General average of a student in one period (all subjects). */
    public static OptionalDouble periodAverage(List<AcademicRecord> records, int year, String period) {
        return records.stream()
                .filter(r -> r.year() == year && r.period().equals(period))
                .mapToDouble(AcademicRecord::value)
                .average();
    }

    /** General average of the whole course in one subject, per period. */
    public static Map<String, Double> courseAveragePerPeriod(List<AcademicRecord> records) {
        return records.stream()
                .collect(Collectors.groupingBy(AcademicRecord::period, TreeMap::new,
                        Collectors.averagingDouble(AcademicRecord::value)));
    }

    // ---------- attendance ----------

    public void recordAttendance(int studentId, int subjectId, int teacherId, LocalDate date, String status) {
        if (date.isAfter(LocalDate.now())) {
            throw new AppException("No se puede registrar asistencia de una fecha futura.");
        }
        try {
            requireActiveStudent(studentId);
            requireSubject(subjectId);
            if (!teacherDao.isTeacherAssigned(teacherId, subjectId, date.getYear())) {
                throw new AppException("El profesor no tiene asignada esa materia en " + date.getYear() + ".");
            }
            academicDao.saveAttendance(studentId, subjectId, date, status, teacherId);
        } catch (SQLException e) {
            AppLogger.error("Error saving attendance for student " + studentId, e);
            throw new AppException("No se pudo registrar la asistencia.", e);
        }
    }

    /** Students with more than 3 absences in a single week during the last {@code days} days. */
    public List<AbsenceRow> absenceReport(int days) {
        try {
            return academicDao.findAbsenceReport(LocalDate.now().minusDays(days),
                    AppConfig.MAX_ABSENCES_PER_WEEK);
        } catch (SQLException e) {
            AppLogger.error("Error building absence report", e);
            throw new AppException("No se pudo generar el reporte de inasistencias.", e);
        }
    }

    // ---------- helpers ----------

    private Student requireStudent(int studentId) throws SQLException {
        return studentDao.findById(studentId).orElseThrow(
                () -> new AppException("No existe un estudiante con ID " + studentId + "."));
    }

    private Student requireActiveStudent(int studentId) throws SQLException {
        Student student = requireStudent(studentId);
        if (!"Activo".equals(student.getStatus())) {
            throw new AppException("El estudiante no esta activo (estado: " + student.getStatus() + ").");
        }
        return student;
    }

    private Subject requireSubject(int subjectId) throws SQLException {
        return subjectDao.findById(subjectId).orElseThrow(
                () -> new AppException("No existe una materia activa con ID " + subjectId + "."));
    }
}
