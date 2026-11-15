package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.JdbcHelper;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Business rules for students, guardians and grade enrollment. */
public class StudentController {

    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final EnrollmentDao enrollmentDao;

    public StudentController(StudentDao studentDao, GuardianDao guardianDao, EnrollmentDao enrollmentDao) {
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.enrollmentDao = enrollmentDao;
    }

    /**
     * Registers a student together with a guardian in ONE transaction, because every
     * student must have at least one guardian. A guardian with id 0 is created first.
     */
    public int registerStudent(Student student, Guardian guardian) {
        if (student.getBirthDate().isAfter(LocalDate.now())) {
            throw new AppException("La fecha de nacimiento no puede ser futura.");
        }
        if (student.getAdmissionDate().isBefore(student.getBirthDate())) {
            throw new AppException("La fecha de ingreso no puede ser anterior a la fecha de nacimiento.");
        }
        try {
            if (studentDao.findByDocument(student.getDocumentNumber()).isPresent()) {
                throw new AppException("Ya existe un estudiante con ese numero de documento.");
            }
            return JdbcHelper.inTransaction(conn -> {
                int guardianId = guardian.id() > 0 ? guardian.id() : guardianDao.insert(conn, guardian);
                int studentId = studentDao.insert(conn, student);
                guardianDao.link(conn, studentId, guardianId, true);
                return studentId;
            });
        } catch (SQLException e) {
            AppLogger.error("Error registering student " + student.getDocumentNumber(), e);
            throw new AppException("No se pudo registrar el estudiante. Revise error_log.txt.", e);
        }
    }

    public Optional<Guardian> findGuardianByDocument(String documentNumber) {
        try {
            return guardianDao.findByDocument(documentNumber);
        } catch (SQLException e) {
            AppLogger.error("Error finding guardian " + documentNumber, e);
            throw new AppException("No se pudo consultar el acudiente.", e);
        }
    }

    public List<Guardian> listGuardians(int studentId) {
        try {
            return guardianDao.findByStudent(studentId);
        } catch (SQLException e) {
            AppLogger.error("Error listing guardians of student " + studentId, e);
            throw new AppException("No se pudo consultar los acudientes.", e);
        }
    }

    /** Links an extra guardian to an existing student (siblings share a guardian). */
    public void linkGuardian(int studentId, Guardian guardian) {
        try {
            if (guardian.id() > 0 && guardianDao.isLinked(studentId, guardian.id())) {
                throw new AppException("Ese acudiente ya esta vinculado al estudiante.");
            }
            JdbcHelper.inTransaction(conn -> {
                int guardianId = guardian.id() > 0 ? guardian.id() : guardianDao.insert(conn, guardian);
                guardianDao.link(conn, studentId, guardianId, false);
                return guardianId;
            });
        } catch (SQLException e) {
            AppLogger.error("Error linking guardian to student " + studentId, e);
            throw new AppException("No se pudo vincular el acudiente.", e);
        }
    }

    public List<Student> listStudents() {
        try {
            return studentDao.findAll();
        } catch (SQLException e) {
            AppLogger.error("Error listing students", e);
            throw new AppException("No se pudo consultar la lista de estudiantes.", e);
        }
    }

    public Optional<Student> findById(int id) {
        try {
            return studentDao.findById(id);
        } catch (SQLException e) {
            AppLogger.error("Error finding student " + id, e);
            throw new AppException("No se pudo consultar el estudiante.", e);
        }
    }

    public void updateStudent(Student student) {
        try {
            if (!studentDao.update(student)) {
                throw new AppException("No existe un estudiante con ID " + student.getId() + ".");
            }
        } catch (SQLException e) {
            AppLogger.error("Error updating student " + student.getId(), e);
            throw new AppException("No se pudo actualizar el estudiante.", e);
        }
    }

    public void withdrawEnrollment(int studentId, int year) {
        try {
            if (!enrollmentDao.withdraw(studentId, year)) {
                throw new AppException("El estudiante no tiene matricula activa en " + year + ".");
            }
        } catch (SQLException e) {
            AppLogger.error("Error withdrawing enrollment of student " + studentId, e);
            throw new AppException("No se pudo retirar la matricula.", e);
        }
    }

    public void changeStatus(int id, String newStatus) {
        try {
            studentDao.updateStatus(id, newStatus);
        } catch (SQLException e) {
            AppLogger.error("Error updating status of student " + id, e);
            throw new AppException("No se pudo actualizar el estado del estudiante.", e);
        }
    }

    public List<Grade> listGrades() {
        try {
            return enrollmentDao.findGrades();
        } catch (SQLException e) {
            AppLogger.error("Error listing grades", e);
            throw new AppException("No se pudo consultar los grados.", e);
        }
    }

    public void enrollInGrade(int studentId, int gradeId, int year) {
        try {
            if (enrollmentDao.findActiveGradeId(studentId, year).isPresent()) {
                throw new AppException("El estudiante ya tiene una matricula activa en " + year + ".");
            }
            enrollmentDao.enroll(studentId, gradeId, year);
        } catch (SQLException e) {
            AppLogger.error("Error enrolling student " + studentId, e);
            throw new AppException("No se pudo matricular al estudiante.", e);
        }
    }
}
