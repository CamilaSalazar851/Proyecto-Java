package com.littlehorizons.erp.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Small helper that removes JDBC boilerplate. Every statement is a PreparedStatement
 * and every resource is closed with try-with-resources.
 */
public final class JdbcHelper {

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface TransactionWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private JdbcHelper() {
    }

    // ---------- queries ----------

    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            return query(conn, sql, mapper, params);
        }
    }

    public static <T> List<T> query(Connection conn, String sql, RowMapper<T> mapper, Object... params)
            throws SQLException {
        List<T> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
            }
        }
        return result;
    }

    /** Runs a query that returns a single integer value (COUNT, MAX, ...). */
    public static long queryLong(Connection conn, String sql, Object... params) throws SQLException {
        List<Long> values = query(conn, sql, rs -> rs.getLong(1), params);
        return values.isEmpty() ? 0 : values.get(0);
    }

    public static long queryLong(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            return queryLong(conn, sql, params);
        }
    }

    // ---------- updates ----------

    public static int update(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            return update(conn, sql, params);
        }
    }

    public static int update(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Executes an INSERT and returns the generated auto-increment key. */
    public static long insert(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            return insert(conn, sql, params);
        }
    }

    public static long insert(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        }
    }

    // ---------- transactions ----------

    /** Runs the work in one transaction: commit if it succeeds, rollback on any failure. */
    public static <T> T inTransaction(TransactionWork<T> work) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ---------- helpers ----------

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p instanceof LocalDate date) {
                ps.setDate(i + 1, java.sql.Date.valueOf(date));
            } else if (p instanceof LocalTime time) {
                ps.setTime(i + 1, Time.valueOf(time));
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }

    public static LocalDate toLocalDate(java.sql.Date date) {
        return date == null ? null : date.toLocalDate();
    }
}
