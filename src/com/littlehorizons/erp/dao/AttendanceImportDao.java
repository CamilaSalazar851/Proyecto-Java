package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.JdbcHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Data access used by the CSV attendance import. It loads the valid student and subject ids once
 * (so each line is checked in memory) and keeps one connection open to insert the lines one by one.
 */
public class AttendanceImportDao {

    private static final String INSERT = "INSERT INTO asistencias (id_estudiante, id_materia, "
            + "fecha_asistencia, estado_asistencia) VALUES (?, ?, ?, ?)";

    /** Inserts one attendance row at a time. Each insert is committed on its own. */
    public interface AttendanceWriter extends AutoCloseable {

        /** Returns true if the row was inserted, false if the database rejected it. */
        boolean insert(int studentId, int subjectId, LocalDate date, String status);

        @Override
        void close();
    }

    public Set<Integer> findStudentIds() throws SQLException {
        return new HashSet<>(JdbcHelper.query("SELECT id_estudiante FROM estudiantes", rs -> rs.getInt(1)));
    }

    public Set<Integer> findSubjectIds() throws SQLException {
        return new HashSet<>(JdbcHelper.query("SELECT id_materia FROM materias", rs -> rs.getInt(1)));
    }

    public AttendanceWriter openWriter() throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try {
            return new JdbcAttendanceWriter(conn, conn.prepareStatement(INSERT));
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
    }

    private static final class JdbcAttendanceWriter implements AttendanceWriter {

        private final Connection conn;
        private final PreparedStatement statement;

        JdbcAttendanceWriter(Connection conn, PreparedStatement statement) {
            this.conn = conn;
            this.statement = statement;
        }

        @Override
        public boolean insert(int studentId, int subjectId, LocalDate date, String status) {
            try {
                statement.setInt(1, studentId);
                statement.setInt(2, subjectId);
                statement.setDate(3, java.sql.Date.valueOf(date));
                statement.setString(4, status);
                return statement.executeUpdate() == 1;
            } catch (SQLException e) {
                // Duplicate (student, subject, date) or any other rejection: the line counts as failed.
                return false;
            }
        }

        @Override
        public void close() {
            try {
                statement.close();
                conn.close();
            } catch (SQLException e) {
                // Nothing useful to do while closing.
            }
        }
    }
}
