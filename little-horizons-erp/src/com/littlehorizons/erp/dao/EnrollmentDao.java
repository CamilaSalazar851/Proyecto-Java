package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.util.JdbcHelper;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data access for grados and matriculas (grade enrollment per academic year). */
public class EnrollmentDao {

    public List<Grade> findGrades() throws SQLException {
        return JdbcHelper.query("SELECT id_grado, nombre_grado FROM grados ORDER BY nivel_numerico",
                rs -> new Grade(rs.getInt("id_grado"), rs.getString("nombre_grado")));
    }

    public Optional<Integer> findActiveGradeId(int studentId, int year) throws SQLException {
        return JdbcHelper.query("SELECT id_grado FROM matriculas WHERE id_estudiante = ? "
                + "AND año_academico = ? AND estado_matricula = 'Activa'",
                rs -> rs.getInt(1), studentId, year).stream().findFirst();
    }

    /** Marks the active enrollment of the year as withdrawn. Returns false if there was none. */
    public boolean withdraw(int studentId, int year) throws SQLException {
        return JdbcHelper.update("UPDATE matriculas SET estado_matricula = 'Retirada', fecha_retiro = CURDATE() "
                + "WHERE id_estudiante = ? AND año_academico = ? AND estado_matricula = 'Activa'",
                studentId, year) > 0;
    }

    public void enroll(int studentId, int gradeId, int year) throws SQLException {
        JdbcHelper.update("INSERT INTO matriculas (id_estudiante, id_grado, año_academico, "
                + "estado_matricula, fecha_matricula) VALUES (?, ?, ?, 'Activa', CURDATE())",
                studentId, gradeId, year);
    }
}
