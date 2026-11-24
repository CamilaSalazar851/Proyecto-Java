package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMember;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.util.JdbcHelper;
import com.littlehorizons.erp.util.JdbcHelper.RowMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access for clubs, memberships, events and event participants. */
public class ExtracurricularDao {

    private static final String SELECT_CLUBS =
            "SELECT c.id_club, c.nombre_club, CONCAT(p.primer_nombre, ' ', p.primer_apellido) AS supervisor, "
          + "c.capacidad_maxima, (SELECT COUNT(*) FROM membresias_club m WHERE m.id_club = c.id_club "
          + "AND m.estado_membresia = 'Activa') AS activos, c.horario_reunion "
          + "FROM clubes c LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor "
          + "WHERE c.esta_activo = TRUE ";

    private static final RowMapper<Club> CLUB_MAPPER = rs -> new Club(rs.getInt("id_club"),
            rs.getString("nombre_club"), rs.getString("supervisor"), rs.getInt("capacidad_maxima"),
            rs.getInt("activos"), rs.getString("horario_reunion"));

    private static final String SELECT_EVENTS =
            "SELECT id_evento, nombre_evento, tipo_evento, fecha_evento, lugar, tiene_competencia "
          + "FROM eventos WHERE esta_activo = TRUE ";

    private static final RowMapper<SchoolEvent> EVENT_MAPPER = rs -> new SchoolEvent(rs.getInt("id_evento"),
            rs.getString("nombre_evento"), rs.getString("tipo_evento"),
            JdbcHelper.toLocalDate(rs.getDate("fecha_evento")), rs.getString("lugar"),
            rs.getBoolean("tiene_competencia"));

    // ---------- clubs ----------

    public List<Club> findClubs() throws SQLException {
        return JdbcHelper.query(SELECT_CLUBS + "ORDER BY c.nombre_club", CLUB_MAPPER);
    }

    public Optional<Club> findClubById(int id) throws SQLException {
        return JdbcHelper.query(SELECT_CLUBS + "AND c.id_club = ?", CLUB_MAPPER, id).stream().findFirst();
    }

    public int insertClub(String name, String description, int supervisorId, int capacity, String schedule)
            throws SQLException {
        return (int) JdbcHelper.insert("INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, "
                + "capacidad_maxima, horario_reunion) VALUES (?, ?, ?, ?, ?)",
                name, description, supervisorId, capacity, schedule);
    }

    /** Null parameters keep the current value (COALESCE). */
    public boolean updateClub(int id, String description, Integer supervisorId, Integer capacity, String schedule)
            throws SQLException {
        return JdbcHelper.update("UPDATE clubes SET descripcion = COALESCE(?, descripcion), "
                + "id_profesor_supervisor = COALESCE(?, id_profesor_supervisor), "
                + "capacidad_maxima = COALESCE(?, capacidad_maxima), horario_reunion = COALESCE(?, horario_reunion) "
                + "WHERE id_club = ?", description, supervisorId, capacity, schedule, id) > 0;
    }

    public boolean setClubActive(int id, boolean active) throws SQLException {
        return JdbcHelper.update("UPDATE clubes SET esta_activo = ? WHERE id_club = ?", active, id) > 0;
    }

    public boolean setEventActive(int id, boolean active) throws SQLException {
        return JdbcHelper.update("UPDATE eventos SET esta_activo = ? WHERE id_evento = ?", active, id) > 0;
    }

    public boolean clubNameExists(String name) throws SQLException {
        return JdbcHelper.queryLong("SELECT COUNT(*) FROM clubes WHERE nombre_club = ?", name) > 0;
    }

    /** Locks the club row so two enrollments cannot both take the last seat. */
    public Optional<Integer> lockClubCapacity(Connection conn, int clubId) throws SQLException {
        return JdbcHelper.query(conn, "SELECT capacidad_maxima FROM clubes WHERE id_club = ? "
                + "AND esta_activo = TRUE FOR UPDATE", rs -> rs.getInt(1), clubId).stream().findFirst();
    }

    public long countActiveMembers(Connection conn, int clubId) throws SQLException {
        return JdbcHelper.queryLong(conn, "SELECT COUNT(*) FROM membresias_club WHERE id_club = ? "
                + "AND estado_membresia = 'Activa'", clubId);
    }

    public boolean isActiveMember(Connection conn, int studentId, int clubId) throws SQLException {
        return JdbcHelper.queryLong(conn, "SELECT COUNT(*) FROM membresias_club WHERE id_estudiante = ? "
                + "AND id_club = ? AND estado_membresia = 'Activa'", studentId, clubId) > 0;
    }

    public void addMember(Connection conn, int studentId, int clubId, LocalDate joinDate) throws SQLException {
        JdbcHelper.update(conn, "INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso, "
                + "estado_membresia) VALUES (?, ?, ?, 'Activa')", studentId, clubId, joinDate);
    }

    public boolean withdrawMember(int studentId, int clubId, String reason) throws SQLException {
        return JdbcHelper.update("UPDATE membresias_club SET estado_membresia = 'Retirada', "
                + "fecha_retiro = CURDATE(), motivo_retiro = ? WHERE id_estudiante = ? AND id_club = ? "
                + "AND estado_membresia = 'Activa'", reason, studentId, clubId) > 0;
    }

    public List<ClubMember> findMembers(int clubId) throws SQLException {
        return JdbcHelper.query("SELECT e.id_estudiante, CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS nombre, "
                + "m.fecha_ingreso, m.estado_membresia FROM membresias_club m "
                + "JOIN estudiantes e ON e.id_estudiante = m.id_estudiante WHERE m.id_club = ? "
                + "ORDER BY m.estado_membresia, e.primer_apellido",
                rs -> new ClubMember(rs.getInt("id_estudiante"), rs.getString("nombre"),
                        JdbcHelper.toLocalDate(rs.getDate("fecha_ingreso")), rs.getString("estado_membresia")),
                clubId);
    }

    // ---------- events ----------

    public List<SchoolEvent> findEvents() throws SQLException {
        return JdbcHelper.query(SELECT_EVENTS + "ORDER BY fecha_evento", EVENT_MAPPER);
    }

    public Optional<SchoolEvent> findEventById(int id) throws SQLException {
        return JdbcHelper.query(SELECT_EVENTS + "AND id_evento = ?", EVENT_MAPPER, id).stream().findFirst();
    }

    public int insertEvent(String name, String description, String type, LocalDate date, String place,
                           boolean competition, int organizerId) throws SQLException {
        return (int) JdbcHelper.insert("INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, "
                + "fecha_evento, lugar, tiene_competencia, id_organizador) VALUES (?, ?, ?, ?, ?, ?, ?)",
                name, description, type, date, place, competition, organizerId);
    }

    public boolean isParticipant(int eventId, int studentId) throws SQLException {
        return JdbcHelper.queryLong("SELECT COUNT(*) FROM participantes_evento WHERE id_evento = ? "
                + "AND id_estudiante = ?", eventId, studentId) > 0;
    }

    public void addParticipant(int eventId, int studentId, String role, String position) throws SQLException {
        JdbcHelper.update("INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, "
                + "posicion_obtenida) VALUES (?, ?, ?, ?)", eventId, studentId, role, position);
    }

    public List<EventParticipant> findParticipants(int eventId) throws SQLException {
        return JdbcHelper.query("SELECT e.id_estudiante, CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS nombre, "
                + "p.rol_participacion, p.posicion_obtenida FROM participantes_evento p "
                + "JOIN estudiantes e ON e.id_estudiante = p.id_estudiante WHERE p.id_evento = ? "
                + "ORDER BY FIELD(p.posicion_obtenida, '1er Lugar', '2do Lugar', '3er Lugar'), e.primer_apellido",
                rs -> new EventParticipant(rs.getInt("id_estudiante"), rs.getString("nombre"),
                        rs.getString("rol_participacion"), rs.getString("posicion_obtenida")), eventId);
    }
}
