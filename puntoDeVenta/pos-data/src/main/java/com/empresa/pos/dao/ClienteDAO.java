package com.empresa.pos.dao;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the clientes table.
 */
public class ClienteDAO extends BaseDAO {

    public ClienteDAO(DataSource dataSource) {
        super(dataSource);
    }

    // ---------------------------------------------------------------
    // Entity
    // ---------------------------------------------------------------

    public static class Cliente {
        private int id;
        private String codigo;
        private String nombre;
        private String rfc;
        private String tipo;
        private int puntosAcumulados;

        public Cliente() {}

        public int getId()                          { return id; }
        public void setId(int id)                   { this.id = id; }

        public String getCodigo()                   { return codigo; }
        public void setCodigo(String codigo)        { this.codigo = codigo; }

        public String getNombre()                   { return nombre; }
        public void setNombre(String nombre)        { this.nombre = nombre; }

        public String getRfc()                      { return rfc; }
        public void setRfc(String rfc)              { this.rfc = rfc; }

        public String getTipo()                     { return tipo; }
        public void setTipo(String tipo)            { this.tipo = tipo; }

        public int getPuntosAcumulados()            { return puntosAcumulados; }
        public void setPuntosAcumulados(int p)      { this.puntosAcumulados = p; }
    }

    // ---------------------------------------------------------------
    // Operations
    // ---------------------------------------------------------------

    public List<Cliente> findAll() {
        String sql = "SELECT id, codigo, nombre, rfc, tipo, puntos_acumulados FROM clientes ORDER BY nombre";
        List<Cliente> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener clientes", e);
        }
        return list;
    }

    public int insert(Cliente c) {
        String sql = """
                INSERT INTO clientes (codigo, nombre, rfc, tipo, puntos_acumulados)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCodigo());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getRfc());
            ps.setString(4, c.getTipo());
            ps.setInt(5, c.getPuntosAcumulados());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar cliente: " + c.getNombre(), e);
        }
        throw new RuntimeException("No se obtuvo id al insertar cliente: " + c.getNombre());
    }

    public void update(Cliente c) {
        String sql = """
                UPDATE clientes
                   SET codigo = ?, nombre = ?, rfc = ?, tipo = ?, puntos_acumulados = ?
                 WHERE id = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCodigo());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getRfc());
            ps.setString(4, c.getTipo());
            ps.setInt(5, c.getPuntosAcumulados());
            ps.setInt(6, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar cliente id=" + c.getId(), e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM clientes WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar cliente id=" + id, e);
        }
    }

    public void agregarPuntos(int clienteId, int puntos) {
        String sql = "UPDATE clientes SET puntos_acumulados = puntos_acumulados + ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, puntos);
            ps.setInt(2, clienteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al agregar puntos al cliente id=" + clienteId, e);
        }
    }

    // ---------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------

    private Cliente map(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setCodigo(rs.getString("codigo"));
        c.setNombre(rs.getString("nombre"));
        c.setRfc(rs.getString("rfc"));
        c.setTipo(rs.getString("tipo"));
        c.setPuntosAcumulados(rs.getInt("puntos_acumulados"));
        return c;
    }
}
