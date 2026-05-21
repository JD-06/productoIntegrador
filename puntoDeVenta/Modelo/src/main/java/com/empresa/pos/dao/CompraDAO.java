package com.empresa.pos.dao;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the compras and proveedores tables.
 */
public class CompraDAO extends BaseDAO {

    public CompraDAO(DataSource dataSource) {
        super(dataSource);
    }

    // ---------------------------------------------------------------
    // Entities
    // ---------------------------------------------------------------

    public static class Compra {
        private int id;
        private int proveedorId;
        private String proveedorNombre;
        private BigDecimal total;
        private int usuarioId;
        private String usuarioNombre;
        private LocalDateTime creadoEn;

        public int getId()                          { return id; }
        public void setId(int id)                   { this.id = id; }

        public int getProveedorId()                 { return proveedorId; }
        public void setProveedorId(int pid)         { this.proveedorId = pid; }

        public String getProveedorNombre()          { return proveedorNombre; }
        public void setProveedorNombre(String pn)   { this.proveedorNombre = pn; }

        public BigDecimal getTotal()                { return total; }
        public void setTotal(BigDecimal total)      { this.total = total; }

        public int getUsuarioId()                   { return usuarioId; }
        public void setUsuarioId(int uid)           { this.usuarioId = uid; }

        public String getUsuarioNombre()            { return usuarioNombre; }
        public void setUsuarioNombre(String un)     { this.usuarioNombre = un; }

        public LocalDateTime getCreadoEn()          { return creadoEn; }
        public void setCreadoEn(LocalDateTime c)    { this.creadoEn = c; }
    }

    public static class Proveedor {
        private int id;
        private String nombre;
        private String rfc;
        private String contacto;

        public int getId()                          { return id; }
        public void setId(int id)                   { this.id = id; }

        public String getNombre()                   { return nombre; }
        public void setNombre(String nombre)        { this.nombre = nombre; }

        public String getRfc()                      { return rfc; }
        public void setRfc(String rfc)              { this.rfc = rfc; }

        public String getContacto()                 { return contacto; }
        public void setContacto(String contacto)    { this.contacto = contacto; }

        @Override
        public String toString() { return nombre; }
    }

    // ---------------------------------------------------------------
    // Operations
    // ---------------------------------------------------------------

    public List<Compra> findAll() {
        String sql = """
                SELECT c.id, c.proveedor_id, p.nombre AS proveedor_nombre,
                       c.total, c.usuario_id, u.nombre AS usuario_nombre, c.creado_en
                  FROM compras c
                  LEFT JOIN proveedores p ON p.id = c.proveedor_id
                  LEFT JOIN usuarios    u ON u.id = c.usuario_id
                 ORDER BY c.creado_en DESC
                """;
        List<Compra> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Compra c = new Compra();
                c.setId(rs.getInt("id"));
                c.setProveedorId(rs.getInt("proveedor_id"));
                c.setProveedorNombre(rs.getString("proveedor_nombre"));
                c.setTotal(rs.getBigDecimal("total"));
                c.setUsuarioId(rs.getInt("usuario_id"));
                c.setUsuarioNombre(rs.getString("usuario_nombre"));
                Timestamp ts = rs.getTimestamp("creado_en");
                if (ts != null) c.setCreadoEn(ts.toLocalDateTime());
                list.add(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener compras", e);
        }
        return list;
    }

    public int insertCompra(Compra c) {
        String sql = """
                INSERT INTO compras (proveedor_id, total, usuario_id)
                VALUES (?, ?, ?)
                RETURNING id
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (c.getProveedorId() > 0) ps.setInt(1, c.getProveedorId());
            else ps.setNull(1, Types.INTEGER);
            ps.setBigDecimal(2, c.getTotal());
            if (c.getUsuarioId() > 0) ps.setInt(3, c.getUsuarioId());
            else ps.setNull(3, Types.INTEGER);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar compra", e);
        }
        throw new RuntimeException("No se obtuvo id al insertar compra");
    }

    public List<Proveedor> findProveedores() {
        String sql = "SELECT id, nombre, rfc, contacto FROM proveedores ORDER BY nombre";
        List<Proveedor> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Proveedor p = new Proveedor();
                p.setId(rs.getInt("id"));
                p.setNombre(rs.getString("nombre"));
                p.setRfc(rs.getString("rfc"));
                p.setContacto(rs.getString("contacto"));
                list.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener proveedores", e);
        }
        return list;
    }

    public int insertProveedor(String nombre, String rfc, String contacto) {
        String sql = "INSERT INTO proveedores (nombre, rfc, contacto) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, rfc);
            ps.setString(3, contacto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar proveedor: " + nombre, e);
        }
        throw new RuntimeException("No se obtuvo id al insertar proveedor: " + nombre);
    }
}
