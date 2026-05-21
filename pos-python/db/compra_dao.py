from decimal import Decimal
from db.connection import DB


def find_all():
    return DB.fetchall(
        """SELECT c.id, p.nombre AS proveedor, c.total,
                  u.nombre AS usuario, c.creado_en
           FROM compras c
           LEFT JOIN proveedores p ON p.id = c.proveedor_id
           LEFT JOIN usuarios u ON u.id = c.usuario_id
           ORDER BY c.creado_en DESC LIMIT 200"""
    )


def find_proveedores():
    return DB.fetchall("SELECT id, nombre, rfc, contacto FROM proveedores ORDER BY nombre")


def insert_proveedor(nombre, rfc, contacto):
    DB.execute(
        "INSERT INTO proveedores (nombre, rfc, contacto) VALUES (%s,%s,%s)",
        (nombre, rfc, contacto)
    )


def insertar_compra(proveedor_id, usuario_id, items):
    """items: list of {producto_id, cantidad, costo_unitario}"""
    total = sum(Decimal(str(i["cantidad"])) * Decimal(str(i["costo_unitario"])) for i in items)
    conn = DB.conn()
    try:
        with conn.cursor() as cur:
            cur.execute(
                """INSERT INTO compras (proveedor_id, total, usuario_id)
                   VALUES (%s,%s,%s) RETURNING id""",
                (proveedor_id, total, usuario_id)
            )
            compra_id = cur.fetchone()[0]
            for item in items:
                cur.execute(
                    """INSERT INTO detalle_compras (compra_id, producto_id, cantidad, costo_unitario)
                       VALUES (%s,%s,%s,%s)""",
                    (compra_id, item["producto_id"], item["cantidad"], item["costo_unitario"])
                )
                cur.execute(
                    "UPDATE inventario SET stock_actual = stock_actual + %s WHERE producto_id=%s",
                    (item["cantidad"], item["producto_id"])
                )
        conn.commit()
        return compra_id
    except Exception:
        conn.rollback()
        raise
