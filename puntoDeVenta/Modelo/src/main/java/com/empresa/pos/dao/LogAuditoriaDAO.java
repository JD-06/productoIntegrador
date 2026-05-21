package com.empresa.pos.dao;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the log_auditoria table.
 */
public class LogAuditoriaDAO extends BaseDAO {

    public LogAuditoriaDAO(DataSource dataSource) {
        super(dataSource);
    }

    // ---------------------------------------------------------------
    // Entity
    // ---------------------------------------------------------------

    public static class LogEntry {
        private int id;
        private String usuarioNombre;
        private String accion;
        private String tablaAfectada;
        private int registroId;
        private LocalDateTime creadoEn;

        public int getId()                              { return id; }
        public void setId(int id)                       { this.id = id; }

        public String getUsuarioNombre()                { return usuarioNombre; }
        public void setUsuarioNombre(String un)         { this.usuarioNombre = un; }

        public String getAccion()                       { return accion; }
        public void setAccion(String accion)            { this.accion = accion; }

        public String getTablaAfectada()                { return tablaAfectada; }
        public void setTablaAfectada(String ta)         { this.tablaAfectada = ta; }

        public int getRegistroId()                      { return registroId; }
        public void setRegistroId(int rid)              { this.registroId = rid; }

        public LocalDateTime getCreadoEn()              { return creadoEn; }
        public void setCreadoEn(LocalDateTime c)        { this.creadoEn = c; }
    }

    // ---------------------------------------------------------------
    // Operations
    // ---------------------------------------------------------------

    private static final String BASE_SELECT = """
            SELECT l.id, u.nombre AS usuario_nombre,
                   l.accion, l.tabla_afectada, l.registro_id, l.creado_en
              FROM log_auditoria l
              LEFT JOIN usuarios u ON u.id = l.usuario_id
            """;

    public List<LogEntry> findAll() {
        List<LogEntry> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT + " ORDER BY l.creado_en DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener log de auditoria", e);
        }
        return list;
    }

    public List<LogEntry> findRecientes(int limit) {
        String sql = BASE_SELECT + " ORDER BY l.creado_en DESC LIMIT ?";
        List<LogEntry> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener log reciente", e);
        }
        return list;
    }

    public void registrar(int usuarioId, String accion, String tabla, int registroId) {
        String sql = """
                INSERT INTO log_auditoria (usuario_id, accion, tabla_afectada, registro_id)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (usuarioId > 0) ps.setInt(1, usuarioId);
            else ps.setNull(1, Types.INTEGER);
            ps.setString(2, accion);
            ps.setString(3, tabla);
            if (registroId > 0) ps.setInt(4, registroId);
            else ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Audit logging failures should not propagate to the caller
            log.error("Error al registrar auditoria: accion={}, tabla={}, registro={}", accion, tabla, registroId, e);
        }
    }

    // ---------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------

    private LogEntry map(ResultSet rs) throws SQLException {
        LogEntry e = new LogEntry();
        e.setId(rs.getInt("id"));
        e.setUsuarioNombre(rs.getString("usuario_nombre"));
        e.setAccion(rs.getString("accion"));
        e.setTablaAfectada(rs.getString("tabla_afectada"));
        e.setRegistroId(rs.getInt("registro_id"));
        Timestamp ts = rs.getTimestamp("creado_en");
        if (ts != null) e.setCreadoEn(ts.toLocalDateTime());
        return e;
    }
}
