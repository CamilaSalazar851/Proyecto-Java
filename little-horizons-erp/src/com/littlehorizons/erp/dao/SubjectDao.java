package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.JdbcHelper;
import com.littlehorizons.erp.util.JdbcHelper.RowMapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Read access to the materias catalog. */
public class SubjectDao {

    private static final String SELECT =
            "SELECT m.id_materia, m.codigo_materia, m.nombre_materia, m.id_grado, g.nombre_grado, "
          + "m.horas_semanales FROM materias m JOIN grados g ON g.id_grado = m.id_grado "
          + "WHERE m.esta_activa = TRUE";

    private static final RowMapper<Subject> MAPPER = rs -> new Subject(
            rs.getInt("id_materia"), rs.getString("codigo_materia"), rs.getString("nombre_materia"),
            rs.getInt("id_grado"), rs.getString("nombre_grado"), rs.getInt("horas_semanales"));

    public List<Subject> findAll() throws SQLException {
        return JdbcHelper.query(SELECT + " ORDER BY g.nivel_numerico, m.nombre_materia", MAPPER);
    }

    public List<Subject> findByGrade(int gradeId) throws SQLException {
        return JdbcHelper.query(SELECT + " AND m.id_grado = ? ORDER BY m.nombre_materia", MAPPER, gradeId);
    }

    public Optional<Subject> findById(int id) throws SQLException {
        return JdbcHelper.query(SELECT + " AND m.id_materia = ?", MAPPER, id).stream().findFirst();
    }
}
