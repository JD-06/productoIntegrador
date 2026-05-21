package com.empresa.pos.dao;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for cuentas_cobrar and cuentas_pagar tables.
 */
public class CuentaDAO extends BaseDAO {

    public CuentaDAO(DataSource dataSource) {
        super(dataSource);
    }

    // ---------------------------------------------------------------
    // Entities
    // ---------------------------------------------------------------

    public static class CuentaCobrar {
        private int id;
        private int clienteId;
        private String clienteNombre;
        private int ventaId;
        private BigDecimal monto;
        private BigDecimal saldo;
        private String estado;
        private LocalDate vencimiento;
        private LocalDateTime creadoEn;

        public int getId()                          { return id; }
        public void setId(int id)                   { this.id = id; }

        public int getClienteId()                   { return clienteId; }
        public void setClienteId(int cid)           { this.clienteId = cid; }

        public String getClienteNombre()            { return clienteNombre; }
        public void setClienteNombre(String cn)     { this.clienteNombre = cn; }

        public int getVentaId()                     { return ventaId; }
        public void setVentaId(int vid)             { this.ventaId = vid; }

        public BigDecimal getMonto()                { return monto; }
        public void setMonto(BigDecimal monto)      { this.monto = monto; }

        public BigDecimal getSaldo()                { return saldo; }
        public void setSaldo(BigDecimal saldo)      { this.saldo = saldo; }

        public String getEstado()                   { return estado; }
        public void setEstado(String estado)        { this.estado = estado; }

        public LocalDate getVencimiento()           { return vencimiento; }
        public void setVencimiento(LocalDate v)     { this.vencimiento = v; }

        public LocalDateTime getCreadoEn()          { return creadoEn; }
        public void setCreadoEn(LocalDateTime c)    { this.creadoEn = c; }
    }

    public static class CuentaPagar {
        private int id;
        private int proveedorId;
        private String proveedorNombre;
        private int compraId;
        private BigDecimal monto;
        private BigDecimal saldo;
        private String estado;
        private LocalDate vencimiento;
        private LocalDateTime creadoEn;

        public int getId()                          { return id; }
        public void setId(int id)                   { this.id = id; }

        public int getProveedorId()                 { return proveedorId; }
        public void setProveedorId(int pid)         { this.proveedorId = pid; }

        public String getProveedorNombre()          { return proveedorNombre; }
        public void setProveedorNombre(String pn)   { this.proveedorNombre = pn; }

        public int getCompraId()                    { return compraId; }
        public void setCompraId(int cid)            { this.compraId = cid; }

        public BigDecimal getMonto()                { return monto; }
        public void setMonto(BigDecimal monto)      { this.monto = monto; }

        public BigDecimal getSaldo()                { return saldo; }
        public void setSaldo(BigDecimal saldo)      { this.saldo = saldo; }

        public String getEstado()                   { return estado; }
        public void setEstado(String estado)        { this.estado = estado; }

        public LocalDate getVencimiento()           { return vencimiento; }
        public void setVencimiento(LocalDate v)     { this.vencimiento = v; }

        public LocalDateTime getCreadoEn()          { return creadoEn; }
        public void setCreadoEn(LocalDateTime c)    { this.creadoEn = c; }
    }

    // ---------------------------------------------------------------
    // CxC Operations
    // ---------------------------------------------------------------

    public List<CuentaCobrar> findAllCxC() {
        String sql = """
                SELECT cc.id, cc.cliente_id, cl.nombre AS cliente_nombre,
                       cc.venta_id, cc.monto, cc.saldo, cc.estado,
                       cc.vencimiento, cc.creado_en
                  FROM cuentas_cobrar cc
                  JOIN clientes cl ON cl.id = cc.cliente_id
                 ORDER BY cc.creado_en DESC
                """;
        List<CuentaCobrar> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CuentaCobrar c = new CuentaCobrar();
                c.setId(rs.getInt("id"));
                c.setClienteId(rs.getInt("cliente_id"));
                c.setClienteNombre(rs.getString("cliente_nombre"));
                c.setVentaId(rs.getInt("venta_id"));
                BigDecimal dbMonto = rs.getBigDecimal("monto");
                c.setMonto(dbMonto != null ? dbMonto.divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO);
                BigDecimal dbSaldo = rs.getBigDecimal("saldo");
                c.setSaldo(dbSaldo != null ? dbSaldo.divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO);
                c.setEstado(rs.getString("estado"));
                Date venc = rs.getDate("vencimiento");
                if (venc != null) c.setVencimiento(venc.toLocalDate());
                Timestamp ts = rs.getTimestamp("creado_en");
                if (ts != null) c.setCreadoEn(ts.toLocalDateTime());
                list.add(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener cuentas por cobrar", e);
        }
        return list;
    }

    public int insertCxC(int clienteId, int ventaId, BigDecimal monto, LocalDate vencimiento) {
        String sql = """
                INSERT INTO cuentas_cobrar (cliente_id, venta_id, monto, saldo, estado, vencimiento)
                VALUES (?, ?, ?, ?, 'PENDIENTE', ?)
                RETURNING id
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            if (ventaId > 0) ps.setInt(2, ventaId);
            else ps.setNull(2, Types.INTEGER);
            ps.setBigDecimal(3, monto);
            ps.setBigDecimal(4, monto);
            if (vencimiento != null) ps.setDate(5, Date.valueOf(vencimiento));
            else ps.setNull(5, Types.DATE);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar cuenta por cobrar", e);
        }
        throw new RuntimeException("No se obtuvo id al insertar cuenta por cobrar");
    }

    public void pagarCxC(int id, BigDecimal monto) {
        String sql = """
                UPDATE cuentas_cobrar
                   SET saldo = GREATEST(saldo - ?, 0),
                       estado = CASE WHEN saldo - ? <= 0 THEN 'PAGADA' ELSE estado END
                 WHERE id = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, monto != null ? monto.multiply(new java.math.BigDecimal("100")) : java.math.BigDecimal.ZERO);
            ps.setBigDecimal(2, monto != null ? monto.multiply(new java.math.BigDecimal("100")) : java.math.BigDecimal.ZERO);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar pago de CxC id=" + id, e);
        }
    }

    // ---------------------------------------------------------------
    // CxP Operations
    // ---------------------------------------------------------------

    public List<CuentaPagar> findAllCxP() {
        String sql = """
                SELECT cp.id, cp.proveedor_id, pr.nombre AS proveedor_nombre,
                       cp.compra_id, cp.monto, cp.saldo, cp.estado,
                       cp.vencimiento, cp.creado_en
                  FROM cuentas_pagar cp
                  JOIN proveedores pr ON pr.id = cp.proveedor_id
                 ORDER BY cp.creado_en DESC
                """;
        List<CuentaPagar> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CuentaPagar c = new CuentaPagar();
                c.setId(rs.getInt("id"));
                c.setProveedorId(rs.getInt("proveedor_id"));
                c.setProveedorNombre(rs.getString("proveedor_nombre"));
                c.setCompraId(rs.getInt("compra_id"));
                BigDecimal dbMonto = rs.getBigDecimal("monto");
                c.setMonto(dbMonto != null ? dbMonto.divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO);
                BigDecimal dbSaldo = rs.getBigDecimal("saldo");
                c.setSaldo(dbSaldo != null ? dbSaldo.divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO);
                c.setEstado(rs.getString("estado"));
                Date venc = rs.getDate("vencimiento");
                if (venc != null) c.setVencimiento(venc.toLocalDate());
                Timestamp ts = rs.getTimestamp("creado_en");
                if (ts != null) c.setCreadoEn(ts.toLocalDateTime());
                list.add(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener cuentas por pagar", e);
        }
        return list;
    }

    public int insertCxP(int proveedorId, int compraId, BigDecimal monto, LocalDate vencimiento) {
        String sql = """
                INSERT INTO cuentas_pagar (proveedor_id, compra_id, monto, saldo, estado, vencimiento)
                VALUES (?, ?, ?, ?, 'PENDIENTE', ?)
                RETURNING id
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, proveedorId);
            if (compraId > 0) ps.setInt(2, compraId);
            else ps.setNull(2, Types.INTEGER);
            ps.setBigDecimal(3, monto);
            ps.setBigDecimal(4, monto);
            if (vencimiento != null) ps.setDate(5, Date.valueOf(vencimiento));
            else ps.setNull(5, Types.DATE);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar cuenta por pagar", e);
        }
        throw new RuntimeException("No se obtuvo id al insertar cuenta por pagar");
    }

    public void pagarCxP(int id, BigDecimal monto) {
        String sql = """
                UPDATE cuentas_pagar
                   SET saldo = GREATEST(saldo - ?, 0),
                       estado = CASE WHEN saldo - ? <= 0 THEN 'PAGADA' ELSE estado END
                 WHERE id = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, monto != null ? monto.multiply(new java.math.BigDecimal("100")) : java.math.BigDecimal.ZERO);
            ps.setBigDecimal(2, monto != null ? monto.multiply(new java.math.BigDecimal("100")) : java.math.BigDecimal.ZERO);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar pago de CxP id=" + id, e);
        }
    }
}
