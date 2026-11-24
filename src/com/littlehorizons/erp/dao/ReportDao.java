package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.NamedAmount;
import com.littlehorizons.erp.model.NamedCount;
import com.littlehorizons.erp.util.JdbcHelper;

import java.sql.SQLException;
import java.util.List;

/** Aggregated queries used by the reports module. */
public class ReportDao {

    public List<NamedCount> enrollmentByGrade(int year) throws SQLException {
        return JdbcHelper.query("SELECT g.nombre_grado, COUNT(m.id_matricula) FROM grados g "
                + "LEFT JOIN matriculas m ON m.id_grado = g.id_grado AND m.año_academico = ? "
                + "AND m.estado_matricula = 'Activa' GROUP BY g.id_grado, g.nombre_grado, g.nivel_numerico "
                + "ORDER BY g.nivel_numerico", rs -> new NamedCount(rs.getString(1), rs.getLong(2)), year);
    }

    public List<NamedAmount> incomeByConcept(int year) throws SQLException {
        return JdbcHelper.query("SELECT c.nombre_concepto, SUM(p.monto_pagado), COUNT(*) FROM pagos p "
                + "JOIN conceptos_pago c ON c.id_concepto = p.id_concepto WHERE p.año_academico = ? "
                + "GROUP BY c.id_concepto, c.nombre_concepto ORDER BY SUM(p.monto_pagado) DESC",
                rs -> new NamedAmount(rs.getString(1), rs.getBigDecimal(2), rs.getLong(3)), year);
    }

    public List<NamedCount> participationByEvent() throws SQLException {
        return JdbcHelper.query("SELECT ev.nombre_evento, COUNT(pe.id_participacion) FROM eventos ev "
                + "LEFT JOIN participantes_evento pe ON pe.id_evento = ev.id_evento WHERE ev.esta_activo = TRUE "
                + "GROUP BY ev.id_evento, ev.nombre_evento, ev.fecha_evento ORDER BY ev.fecha_evento",
                rs -> new NamedCount(rs.getString(1), rs.getLong(2)));
    }
}
