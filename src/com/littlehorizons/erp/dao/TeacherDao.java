package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.JdbcHelper;
import com.littlehorizons.erp.util.JdbcHelper.RowMapper;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** Data access for profesores, asignaciones_profesor and horarios_clase. */
public class TeacherDao {

    private static final String SELECT =
            "SELECT id_profesor, tipo_documento, numero_documento, primer_nombre, primer_apellido, "
          + "segundo_apellido, correo_electronico, telefono, especializacion, fecha_contratacion, estado "
          + "FROM profesores";

    private static final RowMapper<Teacher> TEACHER_MAPPER = rs -> new Teacher(
            rs.getInt("id_profesor"), rs.getString("tipo_documento"), rs.getString("numero_documento"),
            rs.getString("primer_nombre"), rs.getString("primer_apellido"), rs.getString("segundo_apellido"),
            rs.getString("correo_electronico"), rs.getString("telefono"), rs.getString("especializacion"),
            JdbcHelper.toLocalDate(rs.getDate("fecha_contratacion")), rs.getString("estado"));

    private static final String SELECT_ASSIGNMENTS =
            "SELECT a.id_asignacion, a.id_profesor, CONCAT(p.primer_nombre, ' ', p.primer_apellido) AS profesor, "
          + "a.id_materia, CONCAT(m.nombre_materia, ' (', g.nombre_grado, ')') AS materia, "
          + "a.año_academico AS anio, a.seccion, "
          + "(SELECT GROUP_CONCAT(CONCAT(h.dia_semana, ' ', TIME_FORMAT(h.hora_inicio, '%H:%i'), '-', "
          + "TIME_FORMAT(h.hora_fin, '%H:%i')) SEPARATOR ', ') FROM horarios_clase h "
          + "WHERE h.id_asignacion = a.id_asignacion) AS horario "
          + "FROM asignaciones_profesor a JOIN profesores p ON p.id_profesor = a.id_profesor "
          + "JOIN materias m ON m.id_materia = a.id_materia JOIN grados g ON g.id_grado = m.id_grado ";

    private static final RowMapper<Assignment> ASSIGNMENT_MAPPER = rs -> new Assignment(
            rs.getInt("id_asignacion"), rs.getInt("id_profesor"), rs.getString("profesor"),
            rs.getInt("id_materia"), rs.getString("materia"), rs.getInt("anio"),
            rs.getString("seccion"), rs.getString("horario"));

    public int insert(Teacher t) throws SQLException {
        String sql = "INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre, "
                   + "primer_apellido, segundo_apellido, correo_electronico, telefono, especializacion, "
                   + "fecha_contratacion, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Activo')";
        return (int) JdbcHelper.insert(sql, t.documentType(), t.documentNumber(), t.firstName(),
                t.firstLastName(), t.secondLastName(), t.email(), t.phone(), t.specialization(), t.hireDate());
    }

    public List<Teacher> findAll() throws SQLException {
        return JdbcHelper.query(SELECT + " ORDER BY primer_apellido, primer_nombre", TEACHER_MAPPER);
    }

    public Optional<Teacher> findById(int id) throws SQLException {
        return JdbcHelper.query(SELECT + " WHERE id_profesor = ?", TEACHER_MAPPER, id).stream().findFirst();
    }

    public boolean existsByDocumentOrEmail(String document, String email) throws SQLException {
        return JdbcHelper.queryLong("SELECT COUNT(*) FROM profesores WHERE numero_documento = ? "
                + "OR correo_electronico = ?", document, email) > 0;
    }

    public boolean update(int id, String email, String phone, String specialization) throws SQLException {
        return JdbcHelper.update("UPDATE profesores SET correo_electronico = ?, telefono = ?, especializacion = ? "
                + "WHERE id_profesor = ?", email, phone, specialization, id) > 0;
    }

    public boolean emailTakenByAnother(int id, String email) throws SQLException {
        return JdbcHelper.queryLong("SELECT COUNT(*) FROM profesores WHERE correo_electronico = ? "
                + "AND id_profesor <> ?", email, id) > 0;
    }

    public boolean updateStatus(int id, String status) throws SQLException {
        return JdbcHelper.update("UPDATE profesores SET estado = ? WHERE id_profesor = ?", status, id) > 0;
    }

    public int assign(int teacherId, int subjectId, int year, String section) throws SQLException {
        return (int) JdbcHelper.insert("INSERT INTO asignaciones_profesor (id_profesor, id_materia, "
                + "año_academico, seccion) VALUES (?, ?, ?, ?)", teacherId, subjectId, year, section);
    }

    public boolean assignmentExists(int teacherId, int subjectId, int year, String section) throws SQLException {
        return JdbcHelper.queryLong("SELECT COUNT(*) FROM asignaciones_profesor WHERE id_profesor = ? "
                + "AND id_materia = ? AND año_academico = ? AND seccion = ?",
                teacherId, subjectId, year, section) > 0;
    }

    public boolean isTeacherAssigned(int teacherId, int subjectId, int year) throws SQLException {
        return JdbcHelper.queryLong("SELECT COUNT(*) FROM asignaciones_profesor WHERE id_profesor = ? "
                + "AND id_materia = ? AND año_academico = ?", teacherId, subjectId, year) > 0;
    }

    public Optional<Assignment> findAssignmentById(int assignmentId) throws SQLException {
        return JdbcHelper.query(SELECT_ASSIGNMENTS + "WHERE a.id_asignacion = ?", ASSIGNMENT_MAPPER,
                assignmentId).stream().findFirst();
    }

    public List<Assignment> findAssignments(int year) throws SQLException {
        return JdbcHelper.query(SELECT_ASSIGNMENTS + "WHERE a.año_academico = ? "
                + "ORDER BY p.primer_apellido, g.nivel_numerico", ASSIGNMENT_MAPPER, year);
    }

    public List<Assignment> findAssignmentsByTeacher(int teacherId, int year) throws SQLException {
        return JdbcHelper.query(SELECT_ASSIGNMENTS + "WHERE a.id_profesor = ? AND a.año_academico = ? "
                + "ORDER BY g.nivel_numerico", ASSIGNMENT_MAPPER, teacherId, year);
    }

    public boolean hasScheduleConflict(int teacherId, int year, String day, LocalTime start, LocalTime end)
            throws SQLException {
        return JdbcHelper.queryLong("SELECT COUNT(*) FROM horarios_clase h JOIN asignaciones_profesor a "
                + "ON a.id_asignacion = h.id_asignacion WHERE a.id_profesor = ? AND a.año_academico = ? "
                + "AND h.dia_semana = ? AND h.hora_inicio < ? AND h.hora_fin > ?",
                teacherId, year, day, end, start) > 0;
    }

    public void insertSchedule(int assignmentId, String day, LocalTime start, LocalTime end) throws SQLException {
        JdbcHelper.update("INSERT INTO horarios_clase (id_asignacion, dia_semana, hora_inicio, hora_fin) "
                + "VALUES (?, ?, ?, ?)", assignmentId, day, start, end);
    }
}
