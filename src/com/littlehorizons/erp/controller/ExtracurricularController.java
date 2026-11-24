package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ExtracurricularDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMember;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.JdbcHelper;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Business rules for clubs and school events. */
public class ExtracurricularController {

    private final ExtracurricularDao dao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public ExtracurricularController(ExtracurricularDao dao, StudentDao studentDao, TeacherDao teacherDao) {
        this.dao = dao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    // ---------- clubs ----------

    public List<Club> listClubs() {
        try {
            return dao.findClubs();
        } catch (SQLException e) {
            AppLogger.error("Error listing clubs", e);
            throw new AppException("No se pudo consultar los clubes.", e);
        }
    }

    public int createClub(String name, String description, int supervisorId, int capacity, String schedule) {
        try {
            if (dao.clubNameExists(name)) {
                throw new AppException("Ya existe un club con ese nombre.");
            }
            if (teacherDao.findById(supervisorId).isEmpty()) {
                throw new AppException("No existe un profesor con ID " + supervisorId + ".");
            }
            return dao.insertClub(name, description, supervisorId, capacity, schedule);
        } catch (SQLException e) {
            AppLogger.error("Error creating club " + name, e);
            throw new AppException("No se pudo crear el club.", e);
        }
    }

    public void updateClub(int clubId, String description, Integer supervisorId, Integer capacity, String schedule) {
        try {
            Club club = requireClub(clubId);
            if (capacity != null && capacity < club.activeMembers()) {
                throw new AppException("El cupo no puede ser menor que los miembros activos (" + club.activeMembers() + ").");
            }
            if (supervisorId != null && teacherDao.findById(supervisorId).isEmpty()) {
                throw new AppException("No existe un profesor con ID " + supervisorId + ".");
            }
            dao.updateClub(clubId, description, supervisorId, capacity, schedule);
        } catch (SQLException e) {
            AppLogger.error("Error updating club " + clubId, e);
            throw new AppException("No se pudo actualizar el club.", e);
        }
    }

    /** Closes a club (soft delete): members and history are kept, but it leaves the active catalog. */
    public void deactivateClub(int clubId) {
        try {
            requireClub(clubId);
            dao.setClubActive(clubId, false);
        } catch (SQLException e) {
            AppLogger.error("Error deactivating club " + clubId, e);
            throw new AppException("No se pudo desactivar el club.", e);
        }
    }

    public void deactivateEvent(int eventId) {
        try {
            requireEvent(eventId);
            dao.setEventActive(eventId, false);
        } catch (SQLException e) {
            AppLogger.error("Error deactivating event " + eventId, e);
            throw new AppException("No se pudo desactivar el evento.", e);
        }
    }

    /** Enrolls a student in a club, refusing the enrollment when the maximum capacity is reached. */
    public void joinClub(int studentId, int clubId) {
        try {
            Student student = studentDao.findById(studentId).orElseThrow(
                    () -> new AppException("No existe un estudiante con ID " + studentId + "."));
            if (!"Activo".equals(student.getStatus())) {
                throw new AppException("El estudiante no esta activo.");
            }
            JdbcHelper.inTransaction(conn -> {
                int capacity = dao.lockClubCapacity(conn, clubId).orElseThrow(
                        () -> new AppException("No existe un club activo con ID " + clubId + "."));
                if (dao.isActiveMember(conn, studentId, clubId)) {
                    throw new AppException("El estudiante ya es miembro activo de ese club.");
                }
                if (dao.countActiveMembers(conn, clubId) >= capacity) {
                    throw new AppException("El club esta lleno (cupo maximo: " + capacity + ").");
                }
                dao.addMember(conn, studentId, clubId, LocalDate.now());
                return null;
            });
        } catch (SQLException e) {
            AppLogger.error("Error enrolling student " + studentId + " in club " + clubId, e);
            throw new AppException("No se pudo inscribir al estudiante en el club.", e);
        }
    }

    public void leaveClub(int studentId, int clubId, String reason) {
        try {
            if (!dao.withdrawMember(studentId, clubId, reason)) {
                throw new AppException("El estudiante no es miembro activo de ese club.");
            }
        } catch (SQLException e) {
            AppLogger.error("Error withdrawing student " + studentId + " from club " + clubId, e);
            throw new AppException("No se pudo retirar al estudiante del club.", e);
        }
    }

    public Club requireClub(int clubId) {
        try {
            return dao.findClubById(clubId).orElseThrow(
                    () -> new AppException("No existe un club activo con ID " + clubId + "."));
        } catch (SQLException e) {
            AppLogger.error("Error finding club " + clubId, e);
            throw new AppException("No se pudo consultar el club.", e);
        }
    }

    public List<ClubMember> listMembers(int clubId) {
        try {
            return dao.findMembers(clubId);
        } catch (SQLException e) {
            AppLogger.error("Error listing members of club " + clubId, e);
            throw new AppException("No se pudo consultar los miembros.", e);
        }
    }

    // ---------- events ----------

    public List<SchoolEvent> listEvents() {
        try {
            return dao.findEvents();
        } catch (SQLException e) {
            AppLogger.error("Error listing events", e);
            throw new AppException("No se pudo consultar los eventos.", e);
        }
    }

    public int createEvent(String name, String description, String type, LocalDate date, String place,
                           boolean competition, int organizerId) {
        try {
            if (teacherDao.findById(organizerId).isEmpty()) {
                throw new AppException("No existe un profesor con ID " + organizerId + ".");
            }
            return dao.insertEvent(name, description, type, date, place, competition, organizerId);
        } catch (SQLException e) {
            AppLogger.error("Error creating event " + name, e);
            throw new AppException("No se pudo crear el evento.", e);
        }
    }

    public SchoolEvent requireEvent(int eventId) {
        try {
            return dao.findEventById(eventId).orElseThrow(
                    () -> new AppException("No existe un evento activo con ID " + eventId + "."));
        } catch (SQLException e) {
            AppLogger.error("Error finding event " + eventId, e);
            throw new AppException("No se pudo consultar el evento.", e);
        }
    }

    /** Registers a participant. The position is only stored when the event has a competition. */
    public void addParticipant(int eventId, int studentId, String position) {
        try {
            SchoolEvent event = requireEvent(eventId);
            if (studentDao.findById(studentId).isEmpty()) {
                throw new AppException("No existe un estudiante con ID " + studentId + ".");
            }
            if (dao.isParticipant(eventId, studentId)) {
                throw new AppException("El estudiante ya esta registrado en ese evento.");
            }
            String role = event.competition() ? "Competidor" : "Asistente";
            dao.addParticipant(eventId, studentId, role, event.competition() ? position : null);
        } catch (SQLException e) {
            AppLogger.error("Error registering participant " + studentId + " in event " + eventId, e);
            throw new AppException("No se pudo registrar al participante.", e);
        }
    }

    public List<EventParticipant> listParticipants(int eventId) {
        try {
            return dao.findParticipants(eventId);
        } catch (SQLException e) {
            AppLogger.error("Error listing participants of event " + eventId, e);
            throw new AppException("No se pudo consultar los participantes.", e);
        }
    }
}
