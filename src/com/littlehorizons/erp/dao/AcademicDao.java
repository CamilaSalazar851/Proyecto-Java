package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AbsenceRow;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.util.JdbcHelper;
import com.littlehorizons.erp.util.JdbcHelper.RowMapper;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Data access for subject enrollment, grades (registros_academicos) and attendance. */
public class AcademicDao {

    private static final String SELECT_RECORDS =
            "SELECT r.id_estudiante, CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS estudiante, "
          + "r.id_materia, m.nombre_materia, r.año_academico AS anio, r.periodo_academico AS periodo, "
          + "r.valor_nota FROM registros_academicos r "
          + "JOIN estudiantes e ON e.id_estudiante = r.id_estudiante "
          + "JOIN materias m ON m.id_materia = r.id_materia ";

    private static final String RECORDS_ORDER =
            " ORDER BY r.año_academico, FIELD(r.periodo_academico, 'I', 'II', 'III', 'IV', 'Final'), m.nombre_materia";

    private static final RowMapper<AcademicRecord> RECORD_MAPPER = rs -> new AcademicRecord(
            rs.getInt("id_estudiante"), rs.getString("estudiante"), rs.getInt("id_materia"),
            rs.getString("nombre_materia"), rs.getInt("anio"), rs.getString("periodo"),
            rs.getDouble("valor_nota"));

    // ---------- subject enrollment ----------

    public boolean isEnrolled(int studentId, int subjectId, int year, String period) throws SQLException {
        return JdbcHelper.queryLong("SELECT COUNT(*) FROM inscripciones_materia WHERE id_estudiante = ? "
                + "AND id_materia = ? AND año_academico = ? AND periodo_academico = ?",
                studentId, subjectId, year, period) > 0;
    }

    public boolean isEnrolledAnyPeriod(int studentId, int subjectId, int year) throws SQLException {
        return JdbcHelper.queryLong("SELECT COUNT(*) FROM inscripciones_materia WHERE id_estudiante = ? "
                + "AND id_materia = ? AND año_academico = ?", studentId, subjectId, year) > 0;
    }

    public void enroll(int studentId, int subjectId, int year, String period, String section) throws SQLException {
        JdbcHelper.update("INSERT INTO inscripciones_materia (id_estudiante, id_materia, año_academico, "
                + "periodo_academico, seccion, fecha_inscripcion, estado) VALUES (?, ?, ?, ?, ?, CURDATE(), 'Activa')",
                studentId, subjectId, year, period, section);
    }

    // ---------- grades ----------

    /** Inserts the grade, or updates it if one already exists for that student/subject/period. */
    public void saveGrade(int studentId, int subjectId, int teacherId, int year, String period, double value)
            throws SQLException {
        int updated = JdbcHelper.update("UPDATE registros_academicos SET valor_nota = ?, id_profesor = ?, "
                + "fecha_registro = CURDATE() WHERE id_estudiante = ? AND id_materia = ? "
                + "AND año_academico = ? AND periodo_academico = ?",
                value, teacherId, studentId, subjectId, year, period);
        if (updated == 0) {
            JdbcHelper.update("INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, "
                    + "año_academico, periodo_academico, valor_nota, fecha_registro) "
                    + "VALUES (?, ?, ?, ?, ?, ?, CURDATE())",
                    studentId, subjectId, teacherId, year, period, value);
        }
    }

    public List<AcademicRecord> findRecordsByStudent(int studentId) throws SQLException {
        return JdbcHelper.query(SELECT_RECORDS + "WHERE r.id_estudiante = ?" + RECORDS_ORDER,
                RECORD_MAPPER, studentId);
    }

    public List<AcademicRecord> findRecordsBySubject(int subjectId, int year) throws SQLException {
        return JdbcHelper.query(SELECT_RECORDS + "WHERE r.id_materia = ? AND r.año_academico = ?" + RECORDS_ORDER,
                RECORD_MAPPER, subjectId, year);
    }

    public List<AcademicRecord> findRecordsByYear(int year) throws SQLException {
        return JdbcHelper.query(SELECT_RECORDS + "WHERE r.año_academico = ?" + RECORDS_ORDER, RECORD_MAPPER, year);
    }

    // ---------- attendance ----------

    public void saveAttendance(int studentId, int subjectId, LocalDate date, String status, int teacherId)
            throws SQLException {
        int updated = JdbcHelper.update("UPDATE asistencias SET estado_asistencia = ?, registrado_por = ? "
                + "WHERE id_estudiante = ? AND id_materia = ? AND fecha_asistencia = ?",
                status, teacherId, studentId, subjectId, date);
        if (updated == 0) {
            JdbcHelper.update("INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, "
                    + "estado_asistencia, registrado_por) VALUES (?, ?, ?, ?, ?)",
                    studentId, subjectId, date, status, teacherId);
        }
    }

    /** Students with more than {@code maxAbsences} absences within one ISO week since the given date. */
    public List<AbsenceRow> findAbsenceReport(LocalDate since, int maxAbsences) throws SQLException {
        String sql = "SELECT e.id_estudiante, CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS estudiante, "
                   + "CONCAT(YEAR(a.fecha_asistencia), '-S', LPAD(WEEK(a.fecha_asistencia, 1), 2, '0')) AS semana, "
                   + "COUNT(*) AS ausencias FROM asistencias a "
                   + "JOIN estudiantes e ON e.id_estudiante = a.id_estudiante "
                   + "WHERE a.estado_asistencia = 'Ausente' AND a.fecha_asistencia >= ? "
                   + "GROUP BY e.id_estudiante, e.primer_nombre, e.primer_apellido, semana "
                   + "HAVING COUNT(*) > ? ORDER BY ausencias DESC, estudiante";
        return JdbcHelper.query(sql, rs -> new AbsenceRow(rs.getInt("id_estudiante"),
                rs.getString("estudiante"), rs.getString("semana"), rs.getInt("ausencias")),
                since, maxAbsences);
    }
}
