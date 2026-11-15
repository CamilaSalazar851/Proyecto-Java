package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Category;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.util.JdbcHelper;
import com.littlehorizons.erp.util.JdbcHelper.RowMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access for recursos, categorias_recurso and the two loan tables. */
public class ResourceDao {

    private static final String SELECT_RESOURCES =
            "SELECT r.id_recurso, r.codigo_recurso, r.nombre_recurso, c.nombre_categoria, "
          + "r.ubicacion_fisica, r.estado_recurso FROM recursos r "
          + "JOIN categorias_recurso c ON c.id_categoria = r.id_categoria ";

    private static final RowMapper<Resource> RESOURCE_MAPPER = rs -> new Resource(rs.getInt("id_recurso"),
            rs.getString("codigo_recurso"), rs.getString("nombre_recurso"), rs.getString("nombre_categoria"),
            rs.getString("ubicacion_fisica"), rs.getString("estado_recurso"));

    private static final RowMapper<Loan> LOAN_MAPPER = rs -> new Loan(rs.getInt(1), rs.getString(2),
            rs.getString(3), rs.getString(4), rs.getString(5), JdbcHelper.toLocalDate(rs.getDate(6)),
            JdbcHelper.toLocalDate(rs.getDate(7)), JdbcHelper.toLocalDate(rs.getDate(8)), rs.getString(9));

    private static final String STUDENT_LOANS =
            "SELECT pe.id_prestamo, 'Estudiante', r.codigo_recurso, r.nombre_recurso, "
          + "CONCAT(e.primer_nombre, ' ', e.primer_apellido), pe.fecha_prestamo, pe.fecha_devolucion_esperada, "
          + "pe.fecha_devolucion_real, pe.estado_prestamo FROM prestamos_estudiante pe "
          + "JOIN recursos r ON r.id_recurso = pe.id_recurso "
          + "JOIN estudiantes e ON e.id_estudiante = pe.id_estudiante ";

    private static final String TEACHER_LOANS =
            "SELECT pp.id_prestamo, 'Profesor', r.codigo_recurso, r.nombre_recurso, "
          + "CONCAT(p.primer_nombre, ' ', p.primer_apellido), pp.fecha_prestamo, pp.fecha_devolucion_esperada, "
          + "pp.fecha_devolucion_real, pp.estado_prestamo FROM prestamos_profesor pp "
          + "JOIN recursos r ON r.id_recurso = pp.id_recurso "
          + "JOIN profesores p ON p.id_profesor = pp.id_profesor ";

    // ---------- resources ----------

    public List<Category> findCategories() throws SQLException {
        return JdbcHelper.query("SELECT id_categoria, nombre_categoria FROM categorias_recurso ORDER BY id_categoria",
                rs -> new Category(rs.getInt(1), rs.getString(2)));
    }

    public List<Resource> findResources(String status) throws SQLException {
        if (status == null) {
            return JdbcHelper.query(SELECT_RESOURCES + "ORDER BY r.codigo_recurso", RESOURCE_MAPPER);
        }
        return JdbcHelper.query(SELECT_RESOURCES + "WHERE r.estado_recurso = ? ORDER BY r.codigo_recurso",
                RESOURCE_MAPPER, status);
    }

    public Optional<Resource> findResourceById(int id) throws SQLException {
        return JdbcHelper.query(SELECT_RESOURCES + "WHERE r.id_recurso = ?", RESOURCE_MAPPER, id)
                .stream().findFirst();
    }

    public boolean codeExists(String code) throws SQLException {
        return JdbcHelper.queryLong("SELECT COUNT(*) FROM recursos WHERE codigo_recurso = ?", code) > 0;
    }

    public int insertResource(String code, String name, int categoryId, String description, String location,
                              LocalDate acquisitionDate) throws SQLException {
        return (int) JdbcHelper.insert("INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, "
                + "descripcion, fecha_adquisicion, ubicacion_fisica, estado_recurso) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'Disponible')", code, name, categoryId, description,
                acquisitionDate, location);
    }

    /** Null parameters keep the current value (COALESCE). */
    public boolean updateResource(int id, String name, String location, String description) throws SQLException {
        return JdbcHelper.update("UPDATE recursos SET nombre_recurso = COALESCE(?, nombre_recurso), "
                + "ubicacion_fisica = COALESCE(?, ubicacion_fisica), descripcion = COALESCE(?, descripcion) "
                + "WHERE id_recurso = ?", name, location, description, id) > 0;
    }

    public Optional<String> lockResourceStatus(Connection conn, int resourceId) throws SQLException {
        return JdbcHelper.query(conn, "SELECT estado_recurso FROM recursos WHERE id_recurso = ? FOR UPDATE",
                rs -> rs.getString(1), resourceId).stream().findFirst();
    }

    public void setResourceStatus(Connection conn, int resourceId, String status) throws SQLException {
        JdbcHelper.update(conn, "UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?", status, resourceId);
    }

    // ---------- loans ----------

    public void insertStudentLoan(Connection conn, int resourceId, int studentId, LocalDate loanDate,
                                  LocalDate dueDate) throws SQLException {
        JdbcHelper.update(conn, "INSERT INTO prestamos_estudiante (id_recurso, id_estudiante, fecha_prestamo, "
                + "fecha_devolucion_esperada, estado_prestamo) VALUES (?, ?, ?, ?, 'Activo')",
                resourceId, studentId, loanDate, dueDate);
    }

    public void insertTeacherLoan(Connection conn, int resourceId, int teacherId, LocalDate loanDate,
                                  LocalDate dueDate) throws SQLException {
        JdbcHelper.update(conn, "INSERT INTO prestamos_profesor (id_recurso, id_profesor, fecha_prestamo, "
                + "fecha_devolucion_esperada, estado_prestamo) VALUES (?, ?, ?, ?, 'Activo')",
                resourceId, teacherId, loanDate, dueDate);
    }

    /** Closes the loan and returns the id of the resource, or empty if the loan was not open. */
    public Optional<Integer> closeLoan(Connection conn, boolean studentLoan, int loanId) throws SQLException {
        String table = studentLoan ? "prestamos_estudiante" : "prestamos_profesor";
        int updated = JdbcHelper.update(conn, "UPDATE " + table + " SET fecha_devolucion_real = CURDATE(), "
                + "estado_prestamo = 'Devuelto' WHERE id_prestamo = ? AND estado_prestamo IN ('Activo', 'Vencido')",
                loanId);
        if (updated == 0) {
            return Optional.empty();
        }
        return JdbcHelper.query(conn, "SELECT id_recurso FROM " + table + " WHERE id_prestamo = ?",
                rs -> rs.getInt(1), loanId).stream().findFirst();
    }

    /** Flags as 'Vencido' every open loan whose due date has passed. */
    public void markOverdueLoans() throws SQLException {
        for (String table : List.of("prestamos_estudiante", "prestamos_profesor")) {
            JdbcHelper.update("UPDATE " + table + " SET estado_prestamo = 'Vencido' WHERE estado_prestamo = 'Activo' "
                    + "AND fecha_devolucion_real IS NULL AND fecha_devolucion_esperada < CURDATE()");
        }
    }

    public List<Loan> findOpenLoans() throws SQLException {
        return JdbcHelper.query(STUDENT_LOANS + "WHERE pe.estado_prestamo IN ('Activo', 'Vencido') "
                + "UNION ALL " + TEACHER_LOANS + "WHERE pp.estado_prestamo IN ('Activo', 'Vencido') ORDER BY 7",
                LOAN_MAPPER);
    }

    public List<Loan> findOverdueLoans() throws SQLException {
        return JdbcHelper.query(STUDENT_LOANS + "WHERE pe.fecha_devolucion_real IS NULL "
                + "AND pe.fecha_devolucion_esperada < CURDATE() UNION ALL " + TEACHER_LOANS
                + "WHERE pp.fecha_devolucion_real IS NULL AND pp.fecha_devolucion_esperada < CURDATE() ORDER BY 7",
                LOAN_MAPPER);
    }
}
