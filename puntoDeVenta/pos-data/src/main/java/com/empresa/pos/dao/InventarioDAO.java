package com.empresa.pos.dao;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the inventario table.
 */
public class InventarioDAO extends BaseDAO {

    public InventarioDAO(DataSource dataSource) {
        super(dataSource);
    }

    // ---------------------------------------------------------------
    // Entity
    // ---------------------------------------------------------------

    public static class InventarioRow {
        private int productoId;
        private String sku;
        private String nombre;
        private int stockActual;
        private int stockMinimo;

        public int getProductoId()              { return productoId; }
        public void setProductoId(int id)       { this.productoId = id; }

        public String getSku()                  { return sku; }
        public void setSku(String sku)          { this.sku = sku; }

        public String getNombre()               { return nombre; }
        public void setNombre(String nombre)    { this.nombre = nombre; }

        public int getStockActual()             { return stockActual; }
        public void setStockActual(int s)       { this.stockActual = s; }

        public int getStockMinimo()             { return stockMinimo; }
        public void setStockMinimo(int s)       { this.stockMinimo = s; }
    }

    // ---------------------------------------------------------------
    // Operations
    // ---------------------------------------------------------------

    private static final String BASE_SELECT = """
            SELECT i.producto_id, p.sku, p.nombre,
                   i.stock_actual, i.stock_minimo
              FROM inventario i
              JOIN productos p ON p.id = i.producto_id
            """;

    public List<InventarioRow> findAll() {
        List<InventarioRow> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT + " ORDER BY p.nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener inventario", e);
        }
        return list;
    }

    public List<InventarioRow> findBajoStock() {
        String sql = BASE_SELECT + " WHERE i.stock_actual <= i.stock_minimo ORDER BY p.nombre";
        List<InventarioRow> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener productos con bajo stock", e);
        }
        return list;
    }

    /**
     * Adjusts stock by delta amount.
     * @param tipo "ENTRADA" adds, "SALIDA" subtracts.
     */
    public void actualizarStock(int productoId, int cantidad, String tipo) {
        String sql;
        if ("ENTRADA".equalsIgnoreCase(tipo)) {
            sql = "UPDATE inventario SET stock_actual = stock_actual + ? WHERE producto_id = ?";
        } else if ("SALIDA".equalsIgnoreCase(tipo)) {
            sql = "UPDATE inventario SET stock_actual = stock_actual - ? WHERE producto_id = ?";
        } else {
            throw new IllegalArgumentException("Tipo de movimiento invalido: " + tipo);
        }
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, productoId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                log.warn("No se encontro registro de inventario para producto_id={}", productoId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar stock para producto_id=" + productoId, e);
        }
    }

    public InventarioRow getByProducto(int productoId) {
        String sql = BASE_SELECT + " WHERE i.producto_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener inventario de producto_id=" + productoId, e);
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------

    private InventarioRow map(ResultSet rs) throws SQLException {
        InventarioRow row = new InventarioRow();
        row.setProductoId(rs.getInt("producto_id"));
        row.setSku(rs.getString("sku"));
        row.setNombre(rs.getString("nombre"));
        row.setStockActual(rs.getInt("stock_actual"));
        row.setStockMinimo(rs.getInt("stock_minimo"));
        return row;
    }
}
