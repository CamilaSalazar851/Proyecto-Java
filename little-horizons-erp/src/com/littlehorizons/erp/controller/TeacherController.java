package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.AppLogger;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** Business rules for teachers, subject assignment and class schedules. */
public class TeacherController {

    private final TeacherDao teacherDao;
    private final SubjectDao subjectDao;

    public TeacherController(TeacherDao teacherDao, SubjectDao subjectDao) {
        this.teacherDao = teacherDao;
        this.subjectDao = subjectDao;
    }

    public int registerTeacher(Teacher teacher) {
        if (teacher.hireDate().isAfter(LocalDate.now())) {
            throw new AppException("La fecha de contratacion no puede ser futura.");
        }
        try {
            if (teacherDao.existsByDocumentOrEmail(teacher.documentNumber(), teacher.email())) {
                throw new AppException("Ya existe un profesor con ese documento o correo electronico.");
            }
            return teacherDao.insert(teacher);
        } catch (SQLException e) {
            AppLogger.error("Error registering teacher " + teacher.documentNumber(), e);
            throw new AppException("No se pudo registrar el profesor. Revise error_log.txt.", e);
        }
    }

    public void updateTeacher(int id, String email, String phone, String specialization) {
        try {
            if (teacherDao.emailTakenByAnother(id, email)) {
                throw new AppException("Ese correo ya pertenece a otro profesor.");
            }
            if (!teacherDao.update(id, email, phone, specialization)) {
                throw new AppException("No existe un profesor con ID " + id + ".");
            }
        } catch (SQLException e) {
            AppLogger.error("Error updating teacher " + id, e);
            throw new AppException("No se pudo actualizar el profesor.", e);
        }
    }

    /** Deactivates (or reactivates) a teacher. Records are never deleted so history stays intact. */
    public void changeTeacherStatus(int id, String status) {
        try {
            if (!teacherDao.updateStatus(id, status)) {
                throw new AppException("No existe un profesor con ID " + id + ".");
            }
        } catch (SQLException e) {
            AppLogger.error("Error changing status of teacher " + id, e);
            throw new AppException("No se pudo cambiar el estado del profesor.", e);
        }
    }

    public List<Teacher> listTeachers() {
        try {
            return teacherDao.findAll();
        } catch (SQLException e) {
            AppLogger.error("Error listing teachers", e);
            throw new AppException("No se pudo consultar la lista de profesores.", e);
        }
    }

    public Optional<Teacher> findTeacher(int id) {
        try {
            return teacherDao.findById(id);
        } catch (SQLException e) {
            AppLogger.error("Error finding teacher " + id, e);
            throw new AppException("No se pudo consultar el profesor.", e);
        }
    }

    public List<Subject> listSubjects() {
        try {
            return subjectDao.findAll();
        } catch (SQLException e) {
            AppLogger.error("Error listing subjects", e);
            throw new AppException("No se pudo consultar las materias.", e);
        }
    }

    public int assignSubject(int teacherId, int subjectId, int year, String section) {
        try {
            Teacher teacher = teacherDao.findById(teacherId)
                    .orElseThrow(() -> new AppException("No existe un profesor con ID " + teacherId + "."));
            if (!"Activo".equals(teacher.status())) {
                throw new AppException("El profesor no esta activo (estado: " + teacher.status() + ").");
            }
            if (subjectDao.findById(subjectId).isEmpty()) {
                throw new AppException("No existe una materia activa con ID " + subjectId + ".");
            }
            if (teacherDao.assignmentExists(teacherId, subjectId, year, section)) {
                throw new AppException("Esa asignacion ya existe para el anio y la seccion indicados.");
            }
            return teacherDao.assign(teacherId, subjectId, year, section);
        } catch (SQLException e) {
            AppLogger.error("Error assigning subject " + subjectId + " to teacher " + teacherId, e);
            throw new AppException("No se pudo asignar la materia.", e);
        }
    }

    /** Adds a class slot to an assignment, refusing overlaps with the teacher's other classes. */
    public void addSchedule(int assignmentId, String day, LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new AppException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        try {
            Assignment assignment = teacherDao.findAssignmentById(assignmentId)
                    .orElseThrow(() -> new AppException("No existe la asignacion " + assignmentId + "."));
            if (teacherDao.hasScheduleConflict(assignment.teacherId(), assignment.year(), day, start, end)) {
                throw new AppException("Conflicto: el profesor ya tiene una clase que se cruza con ese horario.");
            }
            teacherDao.insertSchedule(assignmentId, day, start, end);
        } catch (SQLException e) {
            AppLogger.error("Error adding schedule to assignment " + assignmentId, e);
            throw new AppException("No se pudo registrar el horario.", e);
        }
    }

    public List<Assignment> listAssignments(int year) {
        try {
            return teacherDao.findAssignments(year);
        } catch (SQLException e) {
            AppLogger.error("Error listing assignments", e);
            throw new AppException("No se pudo consultar las asignaciones.", e);
        }
    }

    public List<Assignment> listAssignmentsByTeacher(int teacherId, int year) {
        try {
            return teacherDao.findAssignmentsByTeacher(teacherId, year);
        } catch (SQLException e) {
            AppLogger.error("Error listing assignments of teacher " + teacherId, e);
            throw new AppException("No se pudo consultar la carga academica.", e);
        }
    }
}
