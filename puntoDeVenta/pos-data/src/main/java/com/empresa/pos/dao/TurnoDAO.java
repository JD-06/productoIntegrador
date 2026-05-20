package com.empresa.pos.dao;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the turnos table.
 */
public class TurnoDAO extends BaseDAO {

    public TurnoDAO(DataSource dataSource) {
        super(dataSource);
    }

    // ---------------------------------------------------------------
    // Entity
    // ---------------------------------------------------------------

    public static class Turno {
        private int id;
        private String codigo;
        private int cajeroId;
        private String cajeroNombre;
        private BigDecimal fondoInicial;
        private BigDecimal efectivoIngresado;
        private BigDecimal cobtoTarjeta;
        private BigDecimal ventaTotal;
        private String estado;
        private LocalDateTime abiertoEn;
        private LocalDateTime cerradoEn;

        public int getId()                              { return id; }
        public void setId(int id)                       { this.id = id; }

        public String getCodigo()                       { return codigo; }
        public void setCodigo(String codigo)            { this.codigo = codigo; }

        public int getCajeroId()                        { return cajeroId; }
        public void setCajeroId(int cajeroId)           { this.cajeroId = cajeroId; }

        public String getCajeroNombre()                 { return cajeroNombre; }
        public void setCajeroNombre(String n)           { this.cajeroNombre = n; }

        public BigDecimal getFondoInicial()             { return fondoInicial; }
        public void setFondoInicial(BigDecimal f)       { this.fondoInicial = f; }

        public BigDecimal getEfectivoIngresado()        { return efectivoIngresado; }
        public void setEfectivoIngresado(BigDecimal e)  { this.efectivoIngresado = e; }

        public BigDecimal getCobtoTarjeta()             { return cobtoTarjeta; }
        public void setCobtoTarjeta(BigDecimal ct)      { this.cobtoTarjeta = ct; }

        public BigDecimal getVentaTotal()               { return ventaTotal; }
        public void setVentaTotal(BigDecimal vt)        { this.ventaTotal = vt; }

        public String getEstado()                       { return estado; }
        public void setEstado(String estado)            { this.estado = estado; }

        public LocalDateTime getAbiertoEn()             { return abiertoEn; }
        public void setAbiertoEn(LocalDateTime a)       { this.abiertoEn = a; }

        public LocalDateTime getCerradoEn()             { return cerradoEn; }
        public void setCerradoEn(LocalDateTime c)       { this.cerradoEn = c; }
    }

    // ---------------------------------------------------------------
    // Operations
    // ---------------------------------------------------------------

    private static final String BASE_SELECT = """
            SELECT t.id, t.codigo, t.cajero_id, u.nombre AS cajero_nombre,
                   t.fondo_inicial, t.efectivo_ingresado, t.cobro_tarjeta,
                   t.venta_total, t.estado, t.abierto_en, t.cerrado_en
              FROM turnos t
              JOIN usuarios u ON u.id = t.cajero_id
            """;

    public List<Turno> findAll() {
        List<Turno> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT + " ORDER BY t.abierto_en DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener turnos", e);
        }
        return list;
    }

    public List<Turno> findActivos() {
        String sql = BASE_SELECT + " WHERE t.estado = 'ABIERTO' ORDER BY t.abierto_en DESC";
        List<Turno> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener turnos activos", e);
        }
        return list;
    }

    public int abrir(int cajeroId, BigDecimal fondoInicial) {
        String sql = """
                INSERT INTO turnos (codigo, cajero_id, fondo_inicial, estado)
                VALUES (?, ?, ?, 'ABIERTO')
                RETURNING id
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, getSiguienteCodigo());
            ps.setInt(2, cajeroId);
            ps.setBigDecimal(3, fondoInicial);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al abrir turno para cajero id=" + cajeroId, e);
        }
        throw new RuntimeException("No se obtuvo id al abrir turno");
    }

    public void cerrar(int turnoId) {
        String sql = """
                UPDATE turnos
                   SET estado = 'CERRADO', cerrado_en = NOW()
                 WHERE id = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, turnoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al cerrar turno id=" + turnoId, e);
        }
    }

    public String getSiguienteCodigo() {
        String sql = "SELECT COUNT(*) + 1 FROM turnos";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return String.format("T-%05d", rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al generar codigo de turno", e);
        }
        return "T-00001";
    }

    // ---------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------

    private Turno map(ResultSet rs) throws SQLException {
        Turno t = new Turno();
        t.setId(rs.getInt("id"));
        t.setCodigo(rs.getString("codigo"));
        t.setCajeroId(rs.getInt("cajero_id"));
        t.setCajeroNombre(rs.getString("cajero_nombre"));
        t.setFondoInicial(rs.getBigDecimal("fondo_inicial"));
        t.setEfectivoIngresado(rs.getBigDecimal("efectivo_ingresado"));
        t.setCobtoTarjeta(rs.getBigDecimal("cobro_tarjeta"));
        t.setVentaTotal(rs.getBigDecimal("venta_total"));
        t.setEstado(rs.getString("estado"));
        Timestamp abierto = rs.getTimestamp("abierto_en");
        if (abierto != null) t.setAbiertoEn(abierto.toLocalDateTime());
        Timestamp cerrado = rs.getTimestamp("cerrado_en");
        if (cerrado != null) t.setCerradoEn(cerrado.toLocalDateTime());
        return t;
    }
}
