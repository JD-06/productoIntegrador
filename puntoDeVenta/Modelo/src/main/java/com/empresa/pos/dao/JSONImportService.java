package com.empresa.pos.dao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * Imports products from a JSON file into the database.
 *
 * <p>Expected JSON structure (array of objects):
 * <pre>
 * [
 *   {
 *     "id": "331170",
 *     "nombre": "Milanesa Nalga",
 *     "marca": "LA HACIENDA",
 *     "categoria": "CarnesCarne VacunaNovillito",
 *     "precio": 16999.0,
 *     "unidad": "kg",
 *     "imagen_local": "prod_0.jpg"
 *   },
 *   ...
 * ]
 * </pre>
 */
public class JSONImportService {

    private static final Logger log = LoggerFactory.getLogger(JSONImportService.class);

    private final CategoriaDAO categoriaDAO;
    private final javax.sql.DataSource dataSource;

    public JSONImportService(CategoriaDAO categoriaDAO, javax.sql.DataSource dataSource) {
        this.categoriaDAO = categoriaDAO;
        this.dataSource   = dataSource;
    }

    // ---------------------------------------------------------------
    // Result holder
    // ---------------------------------------------------------------

    public static class ImportResult {
        private int insertados;
        private int actualizados;
        private int errores;

        public int getInsertados()      { return insertados; }
        public int getActualizados()    { return actualizados; }
        public int getErrores()         { return errores; }

        @Override
        public String toString() {
            return String.format("ImportResult{insertados=%d, actualizados=%d, errores=%d}",
                    insertados, actualizados, errores);
        }
    }

    // ---------------------------------------------------------------
    // JSON DTO
    // ---------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductoJson {
        public String id;
        public String nombre;
        public String marca;
        public String categoria;
        public Double precio;
        public String unidad;
        public String imagen_local;
    }

    // ---------------------------------------------------------------
    // Import
    // ---------------------------------------------------------------

    public ImportResult importar(String jsonPath) {
        ImportResult result = new ImportResult();

        ObjectMapper mapper = new ObjectMapper();
        ProductoJson[] productos;
        try {
            productos = mapper.readValue(new File(jsonPath), ProductoJson[].class);
        } catch (Exception e) {
            throw new RuntimeException("Error al leer JSON desde: " + jsonPath, e);
        }

        log.info("JSON leido: {} productos encontrados en {}", productos.length, jsonPath);

        // ---- 1. Resolve unique categories first (cache to avoid N round-trips) ----
        Map<String, Integer> categoriaCache = new HashMap<>();
        for (ProductoJson p : productos) {
            if (p.categoria != null && !p.categoria.isBlank()) {
                categoriaCache.computeIfAbsent(p.categoria.trim(),
                        nombre -> categoriaDAO.findOrCreate(nombre));
            }
        }
        log.info("Categorias resueltas: {}", categoriaCache.size());

        // ---- 2. Upsert productos in a single transaction ----
        String upsertProducto = """
                INSERT INTO productos (sku, nombre, marca, categoria_id, precio, unidad, activo, imagen_local)
                VALUES (?, ?, ?, ?, ?, ?, true, ?)
                ON CONFLICT (sku) DO UPDATE SET
                    nombre       = EXCLUDED.nombre,
                    marca        = EXCLUDED.marca,
                    categoria_id = EXCLUDED.categoria_id,
                    precio       = EXCLUDED.precio,
                    unidad       = EXCLUDED.unidad,
                    imagen_local = EXCLUDED.imagen_local
                """;

        // xmax = 0 means the row was just inserted (PostgreSQL internal trick)
        String upsertProductoReturning = upsertProducto + " RETURNING xmax";

        String upsertInventario = """
                INSERT INTO inventario (producto_id, stock_actual, stock_minimo)
                SELECT id, 0, 5 FROM productos WHERE sku = ?
                ON CONFLICT (producto_id) DO NOTHING
                """;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psP = conn.prepareStatement(upsertProductoReturning);
                 PreparedStatement psI = conn.prepareStatement(upsertInventario)) {

                int batch = 0;
                for (ProductoJson p : productos) {
                    try {
                        if (p.id == null || p.nombre == null) {
                            log.warn("Producto sin id o nombre, omitido: {}", p.nombre);
                            result.errores++;
                            continue;
                        }

                        String sku = p.id.trim();
                        Integer catId = (p.categoria != null && !p.categoria.isBlank())
                                ? categoriaCache.get(p.categoria.trim())
                                : null;

                        psP.setString(1, sku);
                        psP.setString(2, p.nombre.trim());
                        psP.setString(3, p.marca);
                        if (catId != null) psP.setInt(4, catId);
                        else psP.setNull(4, java.sql.Types.INTEGER);
                        psP.setBigDecimal(5, p.precio != null
                                ? BigDecimal.valueOf(p.precio).multiply(new BigDecimal("100"))
                                : BigDecimal.ZERO);
                        psP.setString(6, p.unidad);
                        psP.setString(7, p.imagen_local);

                        try (java.sql.ResultSet rs = psP.executeQuery()) {
                            if (rs.next()) {
                                long xmax = rs.getLong(1);
                                if (xmax == 0) result.insertados++;
                                else result.actualizados++;
                            }
                        }

                        psI.setString(1, sku);
                        psI.addBatch();
                        batch++;

                        if (batch % 200 == 0) {
                            psI.executeBatch();
                            conn.commit();
                            log.debug("Commit parcial: {} productos procesados", batch);
                        }

                    } catch (Exception ex) {
                        log.error("Error al procesar producto id={}: {}", p.id, ex.getMessage());
                        result.errores++;
                    }
                }

                // Final batch
                psI.executeBatch();
                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Error durante el import — se hizo rollback", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error de conexion durante el import", e);
        }

        log.info("Import completado: {}", result);
        return result;
    }
}
