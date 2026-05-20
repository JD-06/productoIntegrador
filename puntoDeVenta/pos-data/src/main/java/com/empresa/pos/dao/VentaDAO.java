package com.empresa.pos.dao;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the ventas table.
 */
public class VentaDAO extends BaseDAO {

    public VentaDAO(DataSource dataSource) {
        super(dataSource);
    }

    // ---------------------------------------------------------------
    // Entity
    // ---------------------------------------------------------------

    public static class Venta {
        private int id;
        private int turnoId;
        private int cajeroId;
        private String cajeroNombre;
        private int clienteId;
        private String clienteNombre;
        private BigDecimal subtotal;
        private BigDecimal iva;
        private BigDecimal total;
        private int metodoPagoId;
        private String metodoPagoNombre;
        private BigDecimal montoRecibido;
        private BigDecimal cambio;
        private String estado;
        private LocalDateTime creadoEn;

        public int getId()                              { return id; }
        public void setId(int id)                       { this.id = id; }

        public int getTurnoId()                         { return turnoId; }
        public void setTurnoId(int turnoId)             { this.turnoId = turnoId; }

        public int getCajeroId()                        { return cajeroId; }
        public void setCajeroId(int cajeroId)           { this.cajeroId = cajeroId; }

        public String getCajeroNombre()                 { return cajeroNombre; }
        public void setCajeroNombre(String n)           { this.cajeroNombre = n; }

        public int getClienteId()                       { return clienteId; }
        public void setClienteId(int clienteId)         { this.clienteId = clienteId; }

        public String getClienteNombre()                { return clienteNombre; }
        public void setClienteNombre(String n)          { this.clienteNombre = n; }

        public BigDecimal getSubtotal()                 { return subtotal; }
        public void setSubtotal(BigDecimal s)           { this.subtotal = s; }

        public BigDecimal getIva()                      { return iva; }
        public void setIva(BigDecimal iva)              { this.iva = iva; }

        public BigDecimal getTotal()                    { return total; }
        public void setTotal(BigDecimal total)          { this.total = total; }

        public int getMetodoPagoId()                    { return metodoPagoId; }
        public void setMetodoPagoId(int mp)             { this.metodoPagoId = mp; }

        public String getMetodoPagoNombre()             { return metodoPagoNombre; }
        public void setMetodoPagoNombre(String mpn)     { this.metodoPagoNombre = mpn; }

        public BigDecimal getMontoRecibido()            { return montoRecibido; }
        public void setMontoRecibido(BigDecimal mr)     { this.montoRecibido = mr; }

        public BigDecimal getCambio()                   { return cambio; }
        public void setCambio(BigDecimal cambio)        { this.cambio = cambio; }

        public String getEstado()                       { return estado; }
        public void setEstado(String estado)            { this.estado = estado; }

        public LocalDateTime getCreadoEn()              { return creadoEn; }
        public void setCreadoEn(LocalDateTime c)        { this.creadoEn = c; }
    }

    // ---------------------------------------------------------------
    // Base SELECT
    // ---------------------------------------------------------------

    private static final String BASE_SELECT = """
            SELECT v.id, v.turno_id, v.cajero_id, u.nombre AS cajero_nombre,
                   v.cliente_id, cl.nombre AS cliente_nombre,
                   v.subtotal, v.iva, v.total,
                   v.metodo_pago_id, mp.nombre AS metodo_pago_nombre,
                   v.monto_recibido, v.cambio, v.estado, v.creado_en
              FROM ventas v
              JOIN usuarios u     ON u.id  = v.cajero_id
              LEFT JOIN clientes cl ON cl.id = v.cliente_id
              JOIN metodos_pago mp ON mp.id = v.metodo_pago_id
            """;

    // ---------------------------------------------------------------
    // Operations
    // ---------------------------------------------------------------

    public List<Venta> findAll() {
        List<Venta> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT + " ORDER BY v.creado_en DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener ventas", e);
        }
        return list;
    }

    public List<Venta> findByTurno(int turnoId) {
        String sql = BASE_SELECT + " WHERE v.turno_id = ? ORDER BY v.creado_en DESC";
        List<Venta> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, turnoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener ventas del turno id=" + turnoId, e);
        }
        return list;
    }

    public int insert(Venta v) {
        String sql = """
                INSERT INTO ventas
                  (turno_id, cajero_id, cliente_id, subtotal, iva, total,
                   metodo_pago_id, monto_recibido, cambio, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, v.getTurnoId());
            ps.setInt(2, v.getCajeroId());
            if (v.getClienteId() > 0) ps.setInt(3, v.getClienteId());
            else ps.setNull(3, Types.INTEGER);
            ps.setBigDecimal(4, v.getSubtotal());
            ps.setBigDecimal(5, v.getIva());
            ps.setBigDecimal(6, v.getTotal());
            ps.setInt(7, v.getMetodoPagoId());
            ps.setBigDecimal(8, v.getMontoRecibido());
            ps.setBigDecimal(9, v.getCambio());
            ps.setString(10, v.getEstado() != null ? v.getEstado() : "COMPLETADA");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar venta", e);
        }
        throw new RuntimeException("No se obtuvo id al insertar venta");
    }

    public BigDecimal getVentasGlobales() {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM ventas WHERE estado = 'COMPLETADA'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error al calcular ventas globales", e);
        }
        return BigDecimal.ZERO;
    }

    public long getCountCortes() {
        String sql = "SELECT COUNT(*) FROM turnos WHERE estado = 'CERRADO'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar cortes", e);
        }
        return 0L;
    }

    public long getCountTurnosActivos() {
        String sql = "SELECT COUNT(*) FROM turnos WHERE estado = 'ABIERTO'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar turnos activos", e);
        }
        return 0L;
    }

    // ---------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------

    private Venta map(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setId(rs.getInt("id"));
        v.setTurnoId(rs.getInt("turno_id"));
        v.setCajeroId(rs.getInt("cajero_id"));
        v.setCajeroNombre(rs.getString("cajero_nombre"));
        v.setClienteId(rs.getInt("cliente_id"));
        v.setClienteNombre(rs.getString("cliente_nombre"));
        v.setSubtotal(rs.getBigDecimal("subtotal"));
        v.setIva(rs.getBigDecimal("iva"));
        v.setTotal(rs.getBigDecimal("total"));
        v.setMetodoPagoId(rs.getInt("metodo_pago_id"));
        v.setMetodoPagoNombre(rs.getString("metodo_pago_nombre"));
        v.setMontoRecibido(rs.getBigDecimal("monto_recibido"));
        v.setCambio(rs.getBigDecimal("cambio"));
        v.setEstado(rs.getString("estado"));
        Timestamp ts = rs.getTimestamp("creado_en");
        if (ts != null) v.setCreadoEn(ts.toLocalDateTime());
        return v;
    }
}
