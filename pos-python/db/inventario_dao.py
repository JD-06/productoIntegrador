from db.connection import DB


def find_all():
    return DB.fetchall(
        """SELECT p.id AS producto_id, p.sku, p.nombre,
                  COALESCE(i.stock_actual, 0) AS stock_actual,
                  COALESCE(i.stock_minimo, 0) AS stock_minimo,
                  c.nombre AS categoria
           FROM productos p
           LEFT JOIN inventario i ON i.producto_id = p.id
           LEFT JOIN categorias c ON c.id = p.categoria_id
           WHERE p.activo = TRUE
           ORDER BY p.nombre"""
    )


def find_bajo_stock():
    return DB.fetchall(
        """SELECT p.nombre, i.stock_actual, i.stock_minimo
           FROM inventario i
           JOIN productos p ON p.id = i.producto_id
           WHERE i.stock_actual <= i.stock_minimo AND p.activo = TRUE
           ORDER BY i.stock_actual"""
    )


def actualizar_stock(producto_id: int, tipo: str, cantidad: int, usuario_id: int = None):
    """tipo: ENTRADA | SALIDA"""
    if tipo == "ENTRADA":
        DB.execute(
            "UPDATE inventario SET stock_actual = stock_actual + %s WHERE producto_id=%s",
            (cantidad, producto_id)
        )
    else:
        DB.execute(
            "UPDATE inventario SET stock_actual = GREATEST(0, stock_actual - %s) WHERE producto_id=%s",
            (cantidad, producto_id)
        )
    DB.execute(
        """INSERT INTO movimientos_inventario (producto_id, tipo, cantidad, usuario_id)
           VALUES (%s, %s, %s, %s)""",
        (producto_id, tipo, cantidad, usuario_id)
    )


def establecer_stock(producto_id: int, stock: int, minimo: int):
    DB.execute(
        """INSERT INTO inventario (producto_id, stock_actual, stock_minimo)
           VALUES (%s, %s, %s)
           ON CONFLICT (producto_id)
           DO UPDATE SET stock_actual=%s, stock_minimo=%s""",
        (producto_id, stock, minimo, stock, minimo)
    )
