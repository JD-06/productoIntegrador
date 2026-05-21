using Dapper;
using PosMaui.Modelo.Entidad;

namespace PosMaui.Modelo.Dao;

public static class VentaDao
{
    public static int AbrirTurno(int cajeroId, decimal fondoInicial)
    {
        using var conn = ConexionBD.Abrir();
        string codigo = $"T-{DateTime.Now:yyyyMMdd-HHmmss}";
        return conn.QueryFirst<int>("""
            INSERT INTO turnos (codigo, cajero_id, fondo_inicial, estado)
            VALUES (@Codigo, @CajeroId, @Fondo, 'ABIERTO')
            RETURNING id
            """, new { Codigo = codigo, CajeroId = cajeroId, Fondo = fondoInicial });
    }

    public static int GuardarVenta(int turnoId, int cajeroId,
                                    decimal subtotal, decimal iva, decimal total,
                                    int metodoPagoId, decimal montoRecibido, decimal cambio,
                                    List<CartItem> items)
    {
        using var conn = (Npgsql.NpgsqlConnection)ConexionBD.Abrir();
        using var tx = conn.BeginTransaction();
        try
        {
            int ventaId = conn.QueryFirst<int>("""
                INSERT INTO ventas (turno_id, cajero_id, subtotal, iva, total,
                                    metodo_pago_id, monto_recibido, cambio, estado)
                VALUES (@TurnoId, @CajeroId, @Subtotal, @Iva, @Total,
                        @MetodoPagoId, @MontoRecibido, @Cambio, 'COMPLETADA')
                RETURNING id
                """, new { TurnoId = turnoId, CajeroId = cajeroId,
                           Subtotal = subtotal, Iva = iva, Total = total,
                           MetodoPagoId = metodoPagoId, MontoRecibido = montoRecibido,
                           Cambio = cambio }, tx);

            foreach (var item in items)
            {
                conn.Execute("""
                    INSERT INTO detalle_ventas (venta_id, producto_id, cantidad, precio_unitario, subtotal)
                    VALUES (@VentaId, @ProductoId, @Cantidad, @Precio, @Subtotal)
                    """, new { VentaId = ventaId, ProductoId = item.DbProductoId,
                               Cantidad = (int)item.Cantidad, Precio = item.Precio,
                               Subtotal = item.Subtotal }, tx);

                conn.Execute("""
                    UPDATE inventario SET stock_actual = stock_actual - @Cant
                    WHERE producto_id = @PId
                    """, new { Cant = (int)item.Cantidad, PId = item.DbProductoId }, tx);
            }

            tx.Commit();
            return ventaId;
        }
        catch { tx.Rollback(); throw; }
    }

    public static decimal VentasGlobales()
    {
        using var conn = ConexionBD.Abrir();
        return conn.QueryFirst<decimal>(
            "SELECT COALESCE(SUM(total),0) FROM ventas WHERE estado='COMPLETADA'");
    }

    public static int ContarTurnosActivos()
    {
        using var conn = ConexionBD.Abrir();
        return conn.QueryFirst<int>("SELECT COUNT(*) FROM turnos WHERE estado='ABIERTO'");
    }

    public static int ContarCortes()
    {
        using var conn = ConexionBD.Abrir();
        return conn.QueryFirst<int>("SELECT COUNT(*) FROM turnos WHERE estado='CERRADO'");
    }

    public static List<Venta> ObtenerTodas()
    {
        using var conn = ConexionBD.Abrir();
        return conn.Query<Venta>("""
            SELECT v.id AS Id, v.turno_id AS TurnoId, v.cajero_id AS CajeroId,
                   u.nombre AS CajeroNombre, v.subtotal AS Subtotal,
                   v.iva AS Iva, v.total AS Total,
                   mp.nombre AS MetodoPagoNombre,
                   v.monto_recibido AS MontoRecibido, v.cambio AS Cambio,
                   v.estado AS Estado, v.creado_en AS CreadoEn
            FROM ventas v
            JOIN usuarios u ON u.id = v.cajero_id
            JOIN metodos_pago mp ON mp.id = v.metodo_pago_id
            ORDER BY v.creado_en DESC
            LIMIT 100
            """).ToList();
    }
}
