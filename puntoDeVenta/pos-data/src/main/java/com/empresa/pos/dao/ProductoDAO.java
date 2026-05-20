package com.empresa.pos.dao;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the productos table, joined with categorias and inventario.
 */
public class ProductoDAO extends BaseDAO {

    public ProductoDAO(DataSource dataSource) {
        super(dataSource);
    }

    // ---------------------------------------------------------------
    // Entity
    // ---------------------------------------------------------------

    public static class Producto {
        private int id;
        private String sku;
        private String nombre;
        private String marca;
        private String categoria;
        private int categoriaId;
        private BigDecimal precio;
        private String unidad;
        private int stockActual;
        private int stockMinimo;
        private boolean activo;
        private String imagenLocal;

        public Producto() {}

        public int getId()                      { return id; }
        public void setId(int id)               { this.id = id; }

        public String getSku()                  { return sku; }
        public void setSku(String sku)          { this.sku = sku; }

        public String getNombre()               { return nombre; }
        public void setNombre(String nombre)    { this.nombre = nombre; }

        public String getMarca()                { return marca; }
        public void setMarca(String marca)      { this.marca = marca; }

        public String getCategoria()            { return categoria; }
        public void setCategoria(String c)      { this.categoria = c; }

        public int getCategoriaId()             { return categoriaId; }
        public void setCategoriaId(int cid)     { this.categoriaId = cid; }

        public BigDecimal getPrecio()           { return precio; }
        public void setPrecio(BigDecimal p)     { this.precio = p; }

        public String getUnidad()               { return unidad; }
        public void setUnidad(String unidad)    { this.unidad = unidad; }

        public int getStockActual()             { return stockActual; }
        public void setStockActual(int s)       { this.stockActual = s; }

        public int getStockMinimo()             { return stockMinimo; }
        public void setStockMinimo(int s)       { this.stockMinimo = s; }

        public boolean isActivo()               { return activo; }
        public void setActivo(boolean activo)   { this.activo = activo; }

        public String getImagenLocal()          { return imagenLocal; }
        public void setImagenLocal(String il)   { this.imagenLocal = il; }
    }

    // ---------------------------------------------------------------
    // Base SELECT
    // ---------------------------------------------------------------

    private static final String BASE_SELECT = """
            SELECT p.id, p.sku, p.nombre, p.marca, p.precio, p.unidad,
                   p.activo, p.imagen_local,
                   p.categoria_id,
                   c.nombre AS categoria_nombre,
                   COALESCE(i.stock_actual, 0) AS stock_actual,
                   COALESCE(i.stock_minimo, 0) AS stock_minimo
              FROM productos p
              LEFT JOIN categorias c ON c.id = p.categoria_id
              LEFT JOIN inventario  i ON i.producto_id = p.id
            """;

    // ---------------------------------------------------------------
    // CRUD
    // ---------------------------------------------------------------

    public List<Producto> findAll() {
        List<Producto> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT + " ORDER BY p.nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener productos", e);
        }
        return list;
    }

    public Producto findById(int id) {
        String sql = BASE_SELECT + " WHERE p.id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar producto id=" + id, e);
        }
        return null;
    }

    public List<Producto> findByCategoria(int catId) {
        String sql = BASE_SELECT + " WHERE p.categoria_id = ? ORDER BY p.nombre";
        List<Producto> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar productos por categoria id=" + catId, e);
        }
        return list;
    }

    public List<Producto> search(String texto) {
        String sql = BASE_SELECT + """
                 WHERE p.nombre ILIKE ?
                    OR p.sku    ILIKE ?
                    OR p.marca  ILIKE ?
                 ORDER BY p.nombre
                """;
        String patron = "%" + texto + "%";
        List<Producto> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar productos con texto: " + texto, e);
        }
        return list;
    }

    /**
     * Inserts or updates a product by SKU.
     * Uses ON CONFLICT (sku) DO UPDATE to keep prices and names fresh.
     */
    public void insert(Producto p) {
        String sql = """
                INSERT INTO productos (sku, nombre, marca, categoria_id, precio, unidad, activo, imagen_local)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (sku) DO UPDATE SET
                    nombre       = EXCLUDED.nombre,
                    marca        = EXCLUDED.marca,
                    categoria_id = EXCLUDED.categoria_id,
                    precio       = EXCLUDED.precio,
                    unidad       = EXCLUDED.unidad,
                    activo       = EXCLUDED.activo,
                    imagen_local = EXCLUDED.imagen_local
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getSku());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getMarca());
            if (p.getCategoriaId() > 0) ps.setInt(4, p.getCategoriaId());
            else ps.setNull(4, Types.INTEGER);
            ps.setBigDecimal(5, p.getPrecio());
            ps.setString(6, p.getUnidad());
            ps.setBoolean(7, p.isActivo());
            ps.setString(8, p.getImagenLocal());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar/actualizar producto sku=" + p.getSku(), e);
        }
    }

    public void update(Producto p) {
        String sql = """
                UPDATE productos
                   SET sku = ?, nombre = ?, marca = ?, categoria_id = ?,
                       precio = ?, unidad = ?, activo = ?, imagen_local = ?
                 WHERE id = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getSku());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getMarca());
            if (p.getCategoriaId() > 0) ps.setInt(4, p.getCategoriaId());
            else ps.setNull(4, Types.INTEGER);
            ps.setBigDecimal(5, p.getPrecio());
            ps.setString(6, p.getUnidad());
            ps.setBoolean(7, p.isActivo());
            ps.setString(8, p.getImagenLocal());
            ps.setInt(9, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar producto id=" + p.getId(), e);
        }
    }

    public void delete(int id) {
        String sql = "UPDATE productos SET activo = false WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar (desactivar) producto id=" + id, e);
        }
    }

    // ---------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------

    private Producto map(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setSku(rs.getString("sku"));
        p.setNombre(rs.getString("nombre"));
        p.setMarca(rs.getString("marca"));
        p.setCategoriaId(rs.getInt("categoria_id"));
        p.setCategoria(rs.getString("categoria_nombre"));
        p.setPrecio(rs.getBigDecimal("precio"));
        p.setUnidad(rs.getString("unidad"));
        p.setStockActual(rs.getInt("stock_actual"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        p.setActivo(rs.getBoolean("activo"));
        p.setImagenLocal(rs.getString("imagen_local"));
        return p;
    }
}
