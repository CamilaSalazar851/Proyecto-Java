package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.JdbcHelper;
import com.littlehorizons.erp.util.JdbcHelper.RowMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data access for the estudiantes table. */
public class StudentDao {

    private static final String SELECT_ALL =
            "SELECT id_estudiante, tipo_documento, numero_documento, primer_nombre, segundo_nombre, "
          + "primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono, "
          + "correo_electronico, fecha_ingreso, estado FROM estudiantes";

    private static final RowMapper<Student> MAPPER = rs -> {
        Student s = new Student();
        s.setId(rs.getInt("id_estudiante"));
        s.setDocumentType(rs.getString("tipo_documento"));
        s.setDocumentNumber(rs.getString("numero_documento"));
        s.setFirstName(rs.getString("primer_nombre"));
        s.setMiddleName(rs.getString("segundo_nombre"));
        s.setFirstLastName(rs.getString("primer_apellido"));
        s.setSecondLastName(rs.getString("segundo_apellido"));
        s.setBirthDate(rs.getDate("fecha_nacimiento").toLocalDate());
        s.setGender(rs.getString("genero"));
        s.setAddress(rs.getString("direccion"));
        s.setPhone(rs.getString("telefono"));
        s.setEmail(rs.getString("correo_electronico"));
        s.setAdmissionDate(rs.getDate("fecha_ingreso").toLocalDate());
        s.setStatus(rs.getString("estado"));
        return s;
    };

    /** Inserts inside an existing transaction and returns the new id. */
    public int insert(Connection conn, Student s) throws SQLException {
        String sql = "INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, "
                   + "segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, genero, "
                   + "direccion, telefono, correo_electronico, fecha_ingreso, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return (int) JdbcHelper.insert(conn, sql, s.getDocumentType(), s.getDocumentNumber(),
                s.getFirstName(), s.getMiddleName(), s.getFirstLastName(), s.getSecondLastName(),
                s.getBirthDate(), s.getGender(), s.getAddress(), s.getPhone(), s.getEmail(),
                s.getAdmissionDate(), s.getStatus());
    }

    public List<Student> findAll() throws SQLException {
        return JdbcHelper.query(SELECT_ALL + " ORDER BY primer_apellido, primer_nombre", MAPPER);
    }

    public Optional<Student> findById(int id) throws SQLException {
        return JdbcHelper.query(SELECT_ALL + " WHERE id_estudiante = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<Student> findByDocument(String documentNumber) throws SQLException {
        return JdbcHelper.query(SELECT_ALL + " WHERE numero_documento = ?", MAPPER, documentNumber)
                .stream().findFirst();
    }

    /** Updates the editable personal data (document and birth date are not editable). */
    public boolean update(Student s) throws SQLException {
        return JdbcHelper.update("UPDATE estudiantes SET primer_nombre = ?, segundo_nombre = ?, "
                + "primer_apellido = ?, segundo_apellido = ?, direccion = ?, telefono = ?, "
                + "correo_electronico = ? WHERE id_estudiante = ?", s.getFirstName(), s.getMiddleName(),
                s.getFirstLastName(), s.getSecondLastName(), s.getAddress(), s.getPhone(), s.getEmail(), s.getId()) > 0;
    }

    public boolean updateStatus(int id, String status) throws SQLException {
        return JdbcHelper.update("UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?", status, id) > 0;
    }
}
