package com.empresa.pos.dao;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the empleados table.
 */
public class EmpleadoDAO extends BaseDAO {

    public EmpleadoDAO(DataSource dataSource) {
        super(dataSource);
    }

    // ---------------------------------------------------------------
    // Entity
    // ---------------------------------------------------------------

    public static class Empleado {
        private int id;
        private int usuarioId;
        private String nombreUsuario;
        private String puesto;
        private BigDecimal salario;
        private boolean activo;

        public int getId()                          { return id; }
        public void setId(int id)                   { this.id = id; }

        public int getUsuarioId()                   { return usuarioId; }
        public void setUsuarioId(int uid)           { this.usuarioId = uid; }

        public String getNombreUsuario()            { return nombreUsuario; }
        public void setNombreUsuario(String nu)     { this.nombreUsuario = nu; }

        public String getPuesto()                   { return puesto; }
        public void setPuesto(String puesto)        { this.puesto = puesto; }

        public BigDecimal getSalario()              { return salario; }
        public void setSalario(BigDecimal salario)  { this.salario = salario; }

        public boolean isActivo()                   { return activo; }
        public void setActivo(boolean activo)       { this.activo = activo; }
    }

    // ---------------------------------------------------------------
    // Operations
    // ---------------------------------------------------------------

    private static final String BASE_SELECT = """
            SELECT e.id, e.usuario_id, u.nombre AS nombre_usuario,
                   e.puesto, e.salario, e.activo
              FROM empleados e
              JOIN usuarios u ON u.id = e.usuario_id
            """;

    public List<Empleado> findAll() {
        List<Empleado> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT + " ORDER BY u.nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener empleados", e);
        }
        return list;
    }

    public int insert(Empleado e) {
        String sql = """
                INSERT INTO empleados (usuario_id, puesto, salario, activo)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, e.getUsuarioId());
            ps.setString(2, e.getPuesto());
            ps.setBigDecimal(3, e.getSalario());
            ps.setBoolean(4, e.isActivo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error al insertar empleado usuario_id=" + e.getUsuarioId(), ex);
        }
        throw new RuntimeException("No se obtuvo id al insertar empleado");
    }

    public void update(Empleado e) {
        String sql = """
                UPDATE empleados
                   SET usuario_id = ?, puesto = ?, salario = ?, activo = ?
                 WHERE id = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, e.getUsuarioId());
            ps.setString(2, e.getPuesto());
            ps.setBigDecimal(3, e.getSalario());
            ps.setBoolean(4, e.isActivo());
            ps.setInt(5, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Error al actualizar empleado id=" + e.getId(), ex);
        }
    }

    public void delete(int id) {
        String sql = "UPDATE empleados SET activo = false WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al desactivar empleado id=" + id, e);
        }
    }

    // ---------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------

    private Empleado map(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();
        e.setId(rs.getInt("id"));
        e.setUsuarioId(rs.getInt("usuario_id"));
        e.setNombreUsuario(rs.getString("nombre_usuario"));
        e.setPuesto(rs.getString("puesto"));
        e.setSalario(rs.getBigDecimal("salario"));
        e.setActivo(rs.getBoolean("activo"));
        return e;
    }
}
