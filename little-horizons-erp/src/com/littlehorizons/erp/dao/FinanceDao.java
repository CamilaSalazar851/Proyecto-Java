package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Delinquent;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.util.JdbcHelper;
import com.littlehorizons.erp.util.JdbcHelper.RowMapper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access for payment concepts, payments, invoices and delinquency queries. */
public class FinanceDao {

    /** Raw data needed to draw an invoice for one payment. */
    public record InvoiceSource(String receipt, BigDecimal amount, LocalDate paymentDate, String conceptName,
                                String period, String studentName, String guardianName,
                                String guardianDocument, String guardianAddress) {
    }

    private static final RowMapper<PaymentConcept> CONCEPT_MAPPER = rs -> new PaymentConcept(
            rs.getInt("id_concepto"), rs.getString("codigo_concepto"), rs.getString("nombre_concepto"),
            rs.getBigDecimal("monto_base"));

    private static final String SELECT_PAYMENTS =
            "SELECT p.id_pago, p.numero_recibo, p.id_estudiante, "
          + "CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS estudiante, c.nombre_concepto, "
          + "p.monto_pagado, p.fecha_pago, p.metodo_pago, p.periodo_academico "
          + "FROM pagos p JOIN estudiantes e ON e.id_estudiante = p.id_estudiante "
          + "JOIN conceptos_pago c ON c.id_concepto = p.id_concepto ";

    private static final RowMapper<Payment> PAYMENT_MAPPER = rs -> new Payment(rs.getInt("id_pago"),
            rs.getString("numero_recibo"), rs.getInt("id_estudiante"), rs.getString("estudiante"),
            rs.getString("nombre_concepto"), rs.getBigDecimal("monto_pagado"),
            JdbcHelper.toLocalDate(rs.getDate("fecha_pago")), rs.getString("metodo_pago"),
            rs.getString("periodo_academico"));

    // ---------- concepts ----------

    public List<PaymentConcept> findConcepts() throws SQLException {
        return JdbcHelper.query("SELECT id_concepto, codigo_concepto, nombre_concepto, monto_base "
                + "FROM conceptos_pago WHERE esta_activo = TRUE ORDER BY id_concepto", CONCEPT_MAPPER);
    }

    public Optional<PaymentConcept> findConceptById(int id) throws SQLException {
        return JdbcHelper.query("SELECT id_concepto, codigo_concepto, nombre_concepto, monto_base "
                + "FROM conceptos_pago WHERE esta_activo = TRUE AND id_concepto = ?", CONCEPT_MAPPER, id)
                .stream().findFirst();
    }

    /** Active students that share the principal guardian of the given student (siblings, including itself). */
    public long countSiblingsOfPrincipalGuardian(int studentId) throws SQLException {
        return JdbcHelper.queryLong("SELECT COUNT(DISTINCT ea2.id_estudiante) FROM estudiante_acudiente ea "
                + "JOIN estudiante_acudiente ea2 ON ea2.id_acudiente = ea.id_acudiente "
                + "JOIN estudiantes e2 ON e2.id_estudiante = ea2.id_estudiante AND e2.estado = 'Activo' "
                + "WHERE ea.id_estudiante = ? AND ea.es_principal = TRUE", studentId);
    }

    public Optional<Integer> findPrincipalGuardianId(int studentId) throws SQLException {
        return JdbcHelper.query("SELECT id_acudiente FROM estudiante_acudiente WHERE id_estudiante = ? "
                + "ORDER BY es_principal DESC LIMIT 1", rs -> rs.getInt(1), studentId).stream().findFirst();
    }

    // ---------- payments ----------

    public String nextReceiptNumber(Connection conn, int year) throws SQLException {
        long next = JdbcHelper.queryLong(conn, "SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(numero_recibo, '-', -1) "
                + "AS UNSIGNED)), 0) + 1 FROM pagos WHERE numero_recibo LIKE ?", "REC-" + year + "-%");
        return String.format("REC-%d-%04d", year, next);
    }

    public int insertPayment(Connection conn, int studentId, int guardianId, int conceptId, BigDecimal amount,
                             LocalDate date, String method, String receipt, int year, String period)
            throws SQLException {
        return (int) JdbcHelper.insert(conn, "INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, "
                + "monto_pagado, fecha_pago, metodo_pago, numero_recibo, año_academico, periodo_academico) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                studentId, guardianId, conceptId, amount, date, method, receipt, year, period);
    }

    public List<Payment> findPaymentsByStudent(int studentId) throws SQLException {
        return JdbcHelper.query(SELECT_PAYMENTS + "WHERE p.id_estudiante = ? ORDER BY p.fecha_pago DESC, p.id_pago DESC",
                PAYMENT_MAPPER, studentId);
    }

    /** Payments of every student linked to the guardian (a guardian may have several children). */
    public List<Payment> findPaymentsByGuardian(int guardianId) throws SQLException {
        return JdbcHelper.query(SELECT_PAYMENTS + "WHERE p.id_estudiante IN (SELECT id_estudiante FROM "
                + "estudiante_acudiente WHERE id_acudiente = ?) ORDER BY p.fecha_pago DESC, p.id_pago DESC",
                PAYMENT_MAPPER, guardianId);
    }

    /** Active students whose last pension payment is older than one month (or who never paid one). */
    public List<Delinquent> findDelinquents() throws SQLException {
        String sql = "SELECT e.id_estudiante, CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS estudiante, "
                   + "CONCAT(a.primer_nombre, ' ', a.primer_apellido) AS acudiente, a.telefono_principal, "
                   + "(SELECT MAX(p.fecha_pago) FROM pagos p JOIN conceptos_pago c ON c.id_concepto = p.id_concepto "
                   + "WHERE p.id_estudiante = e.id_estudiante AND c.codigo_concepto = 'PENSION') AS ultimo "
                   + "FROM estudiantes e "
                   + "LEFT JOIN estudiante_acudiente ea ON ea.id_estudiante = e.id_estudiante AND ea.es_principal = TRUE "
                   + "LEFT JOIN acudientes a ON a.id_acudiente = ea.id_acudiente "
                   + "WHERE e.estado = 'Activo' AND e.fecha_ingreso < DATE_SUB(CURDATE(), INTERVAL 1 MONTH) "
                   + "HAVING ultimo IS NULL OR ultimo < DATE_SUB(CURDATE(), INTERVAL 1 MONTH) "
                   + "ORDER BY ultimo, estudiante";
        return JdbcHelper.query(sql, rs -> new Delinquent(rs.getInt("id_estudiante"), rs.getString("estudiante"),
                rs.getString("acudiente"), rs.getString("telefono_principal"),
                JdbcHelper.toLocalDate(rs.getDate("ultimo"))));
    }

    // ---------- invoices ----------

    public String nextInvoiceNumber(Connection conn) throws SQLException {
        long next = JdbcHelper.queryLong(conn, "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_factura, 4) "
                + "AS UNSIGNED)), 0) + 1 FROM facturas");
        return String.format("FE-%06d", next);
    }

    public void insertInvoice(Connection conn, String number, int paymentId, LocalDate date, String cufe,
                              String fileName, String path) throws SQLException {
        JdbcHelper.update(conn, "INSERT INTO facturas (numero_factura, id_pago, fecha_emision, cufe, "
                + "nombre_archivo_pdf, ruta_archivo) VALUES (?, ?, ?, ?, ?, ?)",
                number, paymentId, date, cufe, fileName, path);
    }

    /** Everything the invoice needs, read back from the database after the payment was stored. */
    public Optional<InvoiceSource> findInvoiceSource(Connection conn, int paymentId) throws SQLException {
        String sql = "SELECT p.numero_recibo, p.monto_pagado, p.fecha_pago, c.nombre_concepto, "
                   + "p.periodo_academico, CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS estudiante, "
                   + "CONCAT_WS(' ', a.primer_nombre, a.segundo_nombre, a.primer_apellido, a.segundo_apellido) AS acudiente, "
                   + "CONCAT(a.tipo_documento, ' ', a.numero_documento) AS documento, a.direccion "
                   + "FROM pagos p JOIN conceptos_pago c ON c.id_concepto = p.id_concepto "
                   + "JOIN estudiantes e ON e.id_estudiante = p.id_estudiante "
                   + "JOIN acudientes a ON a.id_acudiente = p.id_acudiente WHERE p.id_pago = ?";
        return JdbcHelper.query(conn, sql, rs -> new InvoiceSource(rs.getString("numero_recibo"),
                rs.getBigDecimal("monto_pagado"), JdbcHelper.toLocalDate(rs.getDate("fecha_pago")),
                rs.getString("nombre_concepto"), rs.getString("periodo_academico"), rs.getString("estudiante"),
                rs.getString("acudiente"), rs.getString("documento"), rs.getString("direccion")),
                paymentId).stream().findFirst();
    }

    public Optional<String> findInvoicePath(int paymentId) throws SQLException {
        return JdbcHelper.query("SELECT CONCAT(numero_factura, '|', ruta_archivo) FROM facturas WHERE id_pago = ?",
                rs -> rs.getString(1), paymentId).stream().findFirst();
    }
}
