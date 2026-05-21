package com.empresa.pos.dao;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for usuarios, roles, and permisos tables.
 */
public class UsuarioDAO extends BaseDAO {

    public UsuarioDAO(DataSource dataSource) {
        super(dataSource);
    }

    // ---------------------------------------------------------------
    // Entities
    // ---------------------------------------------------------------

    public static class UsuarioRow {
        private int id;
        private String nombre;
        private String rol;
        private int rolId;
        private boolean activo;

        public int getId()                      { return id; }
        public void setId(int id)               { this.id = id; }

        public String getNombre()               { return nombre; }
        public void setNombre(String nombre)    { this.nombre = nombre; }

        public String getRol()                  { return rol; }
        public void setRol(String rol)          { this.rol = rol; }

        public int getRolId()                   { return rolId; }
        public void setRolId(int rolId)         { this.rolId = rolId; }

        public boolean isActivo()               { return activo; }
        public void setActivo(boolean activo)   { this.activo = activo; }
    }

    public static class RolRow {
        private int id;
        private String nombre;

        public int getId()                      { return id; }
        public void setId(int id)               { this.id = id; }

        public String getNombre()               { return nombre; }
        public void setNombre(String nombre)    { this.nombre = nombre; }

        @Override
        public String toString() { return nombre; }
    }

    public static class PermisoRow {
        private int id;
        private String clave;
        private String descripcion;

        public int getId()                          { return id; }
        public void setId(int id)                   { this.id = id; }

        public String getClave()                    { return clave; }
        public void setClave(String clave)          { this.clave = clave; }

        public String getDescripcion()              { return descripcion; }
        public void setDescripcion(String d)        { this.descripcion = d; }

        @Override
        public String toString() { return clave; }
    }

    // ---------------------------------------------------------------
    // Operations
    // ---------------------------------------------------------------

    public List<UsuarioRow> findAll() {
        String sql = """
                SELECT u.id, u.nombre, r.nombre AS rol, u.rol_id, u.activo
                  FROM usuarios u
                  JOIN roles r ON r.id = u.rol_id
                 ORDER BY u.nombre
                """;
        List<UsuarioRow> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UsuarioRow row = new UsuarioRow();
                row.setId(rs.getInt("id"));
                row.setNombre(rs.getString("nombre"));
                row.setRol(rs.getString("rol"));
                row.setRolId(rs.getInt("rol_id"));
                row.setActivo(rs.getBoolean("activo"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener usuarios", e);
        }
        return list;
    }

    public UsuarioRow findById(int id) {
        String sql = """
                SELECT u.id, u.nombre, r.nombre AS rol, u.rol_id, u.activo
                  FROM usuarios u
                  JOIN roles r ON r.id = u.rol_id
                 WHERE u.id = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UsuarioRow row = new UsuarioRow();
                    row.setId(rs.getInt("id"));
                    row.setNombre(rs.getString("nombre"));
                    row.setRol(rs.getString("rol"));
                    row.setRolId(rs.getInt("rol_id"));
                    row.setActivo(rs.getBoolean("activo"));
                    return row;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar usuario id=" + id, e);
        }
        return null;
    }

    public int insert(String nombre, String pinHash, int rolId) {
        String sql = """
                INSERT INTO usuarios (nombre, pin_hash, rol_id)
                VALUES (?, ?, ?)
                RETURNING id
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, pinHash);
            ps.setInt(3, rolId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar usuario: " + nombre, e);
        }
        throw new RuntimeException("No se obtuvo id al insertar usuario");
    }

    public List<String> getPermisosByRol(int rolId) {
        String sql = """
                SELECT p.clave
                  FROM roles_permisos rp
                  JOIN permisos p ON p.id = rp.permiso_id
                 WHERE rp.rol_id = ?
                """;
        List<String> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rolId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("clave"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener permisos del rol id=" + rolId, e);
        }
        return list;
    }

    public List<RolRow> findRoles() {
        String sql = "SELECT id, nombre FROM roles ORDER BY nombre";
        List<RolRow> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RolRow r = new RolRow();
                r.setId(rs.getInt("id"));
                r.setNombre(rs.getString("nombre"));
                list.add(r);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener roles", e);
        }
        return list;
    }

    public List<PermisoRow> findPermisos() {
        String sql = "SELECT id, clave, descripcion FROM permisos ORDER BY clave";
        List<PermisoRow> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PermisoRow p = new PermisoRow();
                p.setId(rs.getInt("id"));
                p.setClave(rs.getString("clave"));
                p.setDescripcion(rs.getString("descripcion"));
                list.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener permisos", e);
        }
        return list;
    }

    public void asignarPermiso(int rolId, int permisoId) {
        String sql = """
                INSERT INTO roles_permisos (rol_id, permiso_id)
                VALUES (?, ?)
                ON CONFLICT (rol_id, permiso_id) DO NOTHING
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rolId);
            ps.setInt(2, permisoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al asignar permiso " + permisoId + " al rol " + rolId, e);
        }
    }

    public void quitarPermiso(int rolId, int permisoId) {
        String sql = "DELETE FROM roles_permisos WHERE rol_id = ? AND permiso_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rolId);
            ps.setInt(2, permisoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al quitar permiso " + permisoId + " del rol " + rolId, e);
        }
    }

    /** Retorna solo usuarios activos — para el combo del login. */
    public List<UsuarioRow> findAllActivos() {
        String sql = """
                SELECT u.id, u.nombre, r.nombre AS rol, u.rol_id, u.activo
                  FROM usuarios u
                  JOIN roles r ON r.id = u.rol_id
                 WHERE u.activo = true
                 ORDER BY u.nombre
                """;
        List<UsuarioRow> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UsuarioRow row = new UsuarioRow();
                row.setId(rs.getInt("id"));
                row.setNombre(rs.getString("nombre"));
                row.setRol(rs.getString("rol"));
                row.setRolId(rs.getInt("rol_id"));
                row.setActivo(rs.getBoolean("activo"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener usuarios activos", e);
        }
        return list;
    }

    /** Verifica credenciales. Retorna el UsuarioRow si son válidas, null si no. */
    public UsuarioRow verificarPin(String nombre, String pinHash) {
        String sql = """
                SELECT u.id, u.nombre, r.nombre AS rol, u.rol_id, u.activo
                  FROM usuarios u
                  JOIN roles r ON r.id = u.rol_id
                 WHERE u.nombre = ? AND u.pin_hash = ? AND u.activo = true
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, pinHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UsuarioRow row = new UsuarioRow();
                    row.setId(rs.getInt("id"));
                    row.setNombre(rs.getString("nombre"));
                    row.setRol(rs.getString("rol"));
                    row.setRolId(rs.getInt("rol_id"));
                    row.setActivo(rs.getBoolean("activo"));
                    return row;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar PIN", e);
        }
        return null;
    }

    /** Activa o desactiva un usuario. */
    public void toggleActivo(int id, boolean activo) {
        String sql = "UPDATE usuarios SET activo = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar estado del usuario id=" + id, e);
        }
    }

    /** Cambia el PIN de un usuario. */
    public void cambiarPin(int id, String nuevoPinHash) {
        String sql = "UPDATE usuarios SET pin_hash = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoPinHash);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar PIN del usuario id=" + id, e);
        }
    }
}
