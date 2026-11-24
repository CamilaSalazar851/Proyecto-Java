package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.util.JdbcHelper;
import com.littlehorizons.erp.util.JdbcHelper.RowMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data access for acudientes and the estudiante_acudiente link table. */
public class GuardianDao {

    private static final String SELECT =
            "SELECT id_acudiente, tipo_documento, numero_documento, primer_nombre, primer_apellido, "
          + "segundo_apellido, parentesco, telefono_principal, correo_electronico, direccion FROM acudientes";

    private static final RowMapper<Guardian> MAPPER = rs -> new Guardian(
            rs.getInt("id_acudiente"), rs.getString("tipo_documento"), rs.getString("numero_documento"),
            rs.getString("primer_nombre"), rs.getString("primer_apellido"), rs.getString("segundo_apellido"),
            rs.getString("parentesco"), rs.getString("telefono_principal"),
            rs.getString("correo_electronico"), rs.getString("direccion"));

    public int insert(Connection conn, Guardian g) throws SQLException {
        String sql = "INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, "
                   + "primer_apellido, segundo_apellido, parentesco, telefono_principal, "
                   + "correo_electronico, direccion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return (int) JdbcHelper.insert(conn, sql, g.documentType(), g.documentNumber(), g.firstName(),
                g.firstLastName(), g.secondLastName(), g.relationship(), g.phone(), g.email(), g.address());
    }

    public Optional<Guardian> findByDocument(String documentNumber) throws SQLException {
        return JdbcHelper.query(SELECT + " WHERE numero_documento = ?", MAPPER, documentNumber)
                .stream().findFirst();
    }

    public void link(Connection conn, int studentId, int guardianId, boolean principal) throws SQLException {
        JdbcHelper.update(conn,
                "INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, "
              + "es_contacto_emergencia) VALUES (?, ?, ?, TRUE)", studentId, guardianId, principal);
    }

    public boolean isLinked(int studentId, int guardianId) throws SQLException {
        return JdbcHelper.queryLong("SELECT COUNT(*) FROM estudiante_acudiente "
                + "WHERE id_estudiante = ? AND id_acudiente = ?", studentId, guardianId) > 0;
    }

    public List<Guardian> findByStudent(int studentId) throws SQLException {
        return JdbcHelper.query(SELECT + " WHERE id_acudiente IN (SELECT id_acudiente FROM "
                + "estudiante_acudiente WHERE id_estudiante = ?)", MAPPER, studentId);
    }
}
