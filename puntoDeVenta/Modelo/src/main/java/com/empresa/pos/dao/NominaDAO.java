package com.empresa.pos.dao;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for periodos_nomina and nomina tables.
 */
public class NominaDAO extends BaseDAO {

    public NominaDAO(DataSource dataSource) {
        super(dataSource);
    }

    // ---------------------------------------------------------------
    // Entities
    // ---------------------------------------------------------------

    public static class PeriodoNomina {
        private int id;
        private String periodo;
        private LocalDateTime creadoEn;

        public int getId()                          { return id; }
        public void setId(int id)                   { this.id = id; }

        public String getPeriodo()                  { return periodo; }
        public void setPeriodo(String periodo)      { this.periodo = periodo; }

        public LocalDateTime getCreadoEn()          { return creadoEn; }
        public void setCreadoEn(LocalDateTime c)    { this.creadoEn = c; }

        @Override
        public String toString() { return periodo; }
    }

    public static class LineaNomina {
        private int id;
        private int periodoId;
        private String periodoNombre;
        private int empleadoId;
        private String empleadoNombre;
        private BigDecimal salarioBase;
        private BigDecimal deducciones;
        private BigDecimal netoPagar;

        public int getId()                              { return id; }
        public void setId(int id)                       { this.id = id; }

        public int getPeriodoId()                       { return periodoId; }
        public void setPeriodoId(int pid)               { this.periodoId = pid; }

        public String getPeriodoNombre()                { return periodoNombre; }
        public void setPeriodoNombre(String pn)         { this.periodoNombre = pn; }

        public int getEmpleadoId()                      { return empleadoId; }
        public void setEmpleadoId(int eid)              { this.empleadoId = eid; }

        public String getEmpleadoNombre()               { return empleadoNombre; }
        public void setEmpleadoNombre(String en)        { this.empleadoNombre = en; }

        public BigDecimal getSalarioBase()              { return salarioBase; }
        public void setSalarioBase(BigDecimal sb)       { this.salarioBase = sb; }

        public BigDecimal getDeducciones()              { return deducciones; }
        public void setDeducciones(BigDecimal d)        { this.deducciones = d; }

        public BigDecimal getNetoPagar()                { return netoPagar; }
        public void setNetoPagar(BigDecimal np)         { this.netoPagar = np; }
    }

    // ---------------------------------------------------------------
    // Operations
    // ---------------------------------------------------------------

    public List<PeriodoNomina> findPeriodos() {
        String sql = "SELECT id, periodo, creado_en FROM periodos_nomina ORDER BY creado_en DESC";
        List<PeriodoNomina> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PeriodoNomina p = new PeriodoNomina();
                p.setId(rs.getInt("id"));
                p.setPeriodo(rs.getString("periodo"));
                Timestamp ts = rs.getTimestamp("creado_en");
                if (ts != null) p.setCreadoEn(ts.toLocalDateTime());
                list.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener periodos de nomina", e);
        }
        return list;
    }

    public List<LineaNomina> findByPeriodo(int periodoId) {
        String sql = """
                SELECT n.id, n.periodo_id, pn.periodo AS periodo_nombre,
                       n.empleado_id, u.nombre AS empleado_nombre,
                       n.salario_base, n.deducciones, n.neto_pagar
                  FROM nomina n
                  JOIN periodos_nomina pn ON pn.id = n.periodo_id
                  JOIN empleados e        ON e.id  = n.empleado_id
                  JOIN usuarios u         ON u.id  = e.usuario_id
                 WHERE n.periodo_id = ?
                 ORDER BY u.nombre
                """;
        List<LineaNomina> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, periodoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LineaNomina l = new LineaNomina();
                    l.setId(rs.getInt("id"));
                    l.setPeriodoId(rs.getInt("periodo_id"));
                    l.setPeriodoNombre(rs.getString("periodo_nombre"));
                    l.setEmpleadoId(rs.getInt("empleado_id"));
                    l.setEmpleadoNombre(rs.getString("empleado_nombre"));
                    l.setSalarioBase(rs.getBigDecimal("salario_base"));
                    l.setDeducciones(rs.getBigDecimal("deducciones"));
                    l.setNetoPagar(rs.getBigDecimal("neto_pagar"));
                    list.add(l);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener nomina del periodo id=" + periodoId, e);
        }
        return list;
    }

    public int crearPeriodo(String periodo) {
        String sql = "INSERT INTO periodos_nomina (periodo) VALUES (?) RETURNING id";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear periodo de nomina: " + periodo, e);
        }
        throw new RuntimeException("No se obtuvo id al crear periodo de nomina");
    }

    public int insertLinea(LineaNomina l) {
        String sql = """
                INSERT INTO nomina (periodo_id, empleado_id, salario_base, deducciones, neto_pagar)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, l.getPeriodoId());
            ps.setInt(2, l.getEmpleadoId());
            ps.setBigDecimal(3, l.getSalarioBase());
            ps.setBigDecimal(4, l.getDeducciones() != null ? l.getDeducciones() : BigDecimal.ZERO);
            ps.setBigDecimal(5, l.getNetoPagar());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar linea de nomina empleado_id=" + l.getEmpleadoId(), e);
        }
        throw new RuntimeException("No se obtuvo id al insertar linea de nomina");
    }
}
