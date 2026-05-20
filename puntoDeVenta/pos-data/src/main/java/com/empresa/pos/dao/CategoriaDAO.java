package com.empresa.pos.dao;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the categorias table.
 */
public class CategoriaDAO extends BaseDAO {

    public CategoriaDAO(DataSource dataSource) {
        super(dataSource);
    }

    // ---------------------------------------------------------------
    // Entity
    // ---------------------------------------------------------------

    public static class Categoria {
        private int id;
        private String nombre;
        private int cantidadProductos;

        public Categoria() {}

        public Categoria(int id, String nombre, int cantidadProductos) {
            this.id = id;
            this.nombre = nombre;
            this.cantidadProductos = cantidadProductos;
        }

        public int getId()                  { return id; }
        public void setId(int id)           { this.id = id; }

        public String getNombre()           { return nombre; }
        public void setNombre(String n)     { this.nombre = n; }

        public int getCantidadProductos()           { return cantidadProductos; }
        public void setCantidadProductos(int c)     { this.cantidadProductos = c; }

        @Override
        public String toString() { return nombre; }
    }

    // ---------------------------------------------------------------
    // CRUD
    // ---------------------------------------------------------------

    public List<Categoria> findAll() {
        String sql = """
                SELECT c.id, c.nombre,
                       COUNT(p.id) AS cantidad_productos
                  FROM categorias c
                  LEFT JOIN productos p ON p.categoria_id = c.id
                 GROUP BY c.id, c.nombre
                 ORDER BY c.nombre
                """;
        List<Categoria> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener categorias", e);
        }
        return list;
    }

    public Categoria findById(int id) {
        String sql = """
                SELECT c.id, c.nombre,
                       COUNT(p.id) AS cantidad_productos
                  FROM categorias c
                  LEFT JOIN productos p ON p.categoria_id = c.id
                 WHERE c.id = ?
                 GROUP BY c.id, c.nombre
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar categoria id=" + id, e);
        }
        return null;
    }

    public int insert(String nombre) {
        String sql = "INSERT INTO categorias (nombre) VALUES (?) RETURNING id";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar categoria: " + nombre, e);
        }
        throw new RuntimeException("No se obtuvo id al insertar categoria: " + nombre);
    }

    public void update(int id, String nombre) {
        String sql = "UPDATE categorias SET nombre = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar categoria id=" + id, e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM categorias WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar categoria id=" + id, e);
        }
    }

    /**
     * Returns existing category id or creates one if not found.
     * Uses INSERT ... ON CONFLICT DO NOTHING to be safe under concurrent imports.
     */
    public int findOrCreate(String nombre) {
        String insert = "INSERT INTO categorias (nombre) VALUES (?) ON CONFLICT (nombre) DO NOTHING";
        String select = "SELECT id FROM categorias WHERE nombre = ?";
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setString(1, nombre);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error en findOrCreate categoria: " + nombre, e);
        }
        throw new RuntimeException("No se pudo obtener id de categoria: " + nombre);
    }

    // ---------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------

    private Categoria map(ResultSet rs) throws SQLException {
        return new Categoria(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getInt("cantidad_productos")
        );
    }
}
