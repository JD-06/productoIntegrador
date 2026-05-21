using Dapper;
using PosMaui.Modelo.Entidad;

namespace PosMaui.Modelo.Dao;

public static class ProductoDao
{
    public static List<Producto> ObtenerTodos()
    {
        using var conn = ConexionBD.Abrir();
        return conn.Query<Producto>("""
            SELECT p.id AS Id, p.sku AS Sku, p.nombre AS Nombre,
                   COALESCE(p.marca,'') AS Marca,
                   COALESCE(c.nombre,'Sin categoria') AS Categoria,
                   COALESCE(p.categoria_id,0) AS CategoriaId,
                   p.precio AS Precio,
                   COALESCE(p.unidad,'PZA') AS Unidad,
                   COALESCE(i.stock_actual,0) AS StockActual,
                   COALESCE(i.stock_minimo,0) AS StockMinimo,
                   p.activo AS Activo,
                   p.imagen_local AS ImagenLocal
            FROM productos p
            LEFT JOIN categorias c ON c.id = p.categoria_id
            LEFT JOIN inventario i ON i.producto_id = p.id
            WHERE p.activo = true
            ORDER BY p.nombre
            """).ToList();
    }

    public static List<Producto> BuscarPorTexto(string texto)
    {
        using var conn = ConexionBD.Abrir();
        return conn.Query<Producto>("""
            SELECT p.id AS Id, p.sku AS Sku, p.nombre AS Nombre,
                   COALESCE(p.marca,'') AS Marca,
                   COALESCE(c.nombre,'Sin categoria') AS Categoria,
                   p.precio AS Precio,
                   COALESCE(p.unidad,'PZA') AS Unidad,
                   COALESCE(i.stock_actual,0) AS StockActual,
                   COALESCE(i.stock_minimo,0) AS StockMinimo,
                   p.activo AS Activo,
                   p.imagen_local AS ImagenLocal
            FROM productos p
            LEFT JOIN categorias c ON c.id = p.categoria_id
            LEFT JOIN inventario i ON i.producto_id = p.id
            WHERE p.activo = true
              AND (LOWER(p.nombre) LIKE @Texto OR LOWER(p.sku) LIKE @Texto OR LOWER(COALESCE(p.marca,'')) LIKE @Texto)
            ORDER BY p.nombre
            LIMIT 80
            """, new { Texto = $"%{texto.ToLower()}%" }).ToList();
    }

    public static List<Producto> ObtenerBajoStock()
    {
        using var conn = ConexionBD.Abrir();
        return conn.Query<Producto>("""
            SELECT p.id AS Id, p.sku AS Sku, p.nombre AS Nombre,
                   COALESCE(c.nombre,'') AS Categoria,
                   i.stock_actual AS StockActual, i.stock_minimo AS StockMinimo
            FROM productos p
            JOIN inventario i ON i.producto_id = p.id
            LEFT JOIN categorias c ON c.id = p.categoria_id
            WHERE i.stock_actual <= i.stock_minimo AND p.activo = true
            ORDER BY i.stock_actual
            """).ToList();
    }

    public static void ActualizarStock(int productoId, int cantidad, string tipo)
    {
        using var conn = ConexionBD.Abrir();
        string op = tipo == "SALIDA" ? "-" : "+";
        conn.Execute($"""
            UPDATE inventario
            SET stock_actual = stock_actual {op} @Cantidad
            WHERE producto_id = @ProductoId
            """, new { Cantidad = cantidad, ProductoId = productoId });
    }
}
