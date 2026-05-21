from decimal import Decimal
from db.connection import DB


def insertar_venta(turno_id, cajero_id, items, subtotal, iva, total,
                   metodo_pago_id, monto_recibido, cambio, cliente_id=None):
    """Inserts venta + detalle_ventas in one transaction. Returns venta_id."""
    conn = DB.conn()
    try:
        with conn.cursor() as cur:
            cur.execute(
                """INSERT INTO ventas
                       (turno_id, cajero_id, cliente_id, subtotal, iva, total,
                        metodo_pago_id, monto_recibido, cambio)
                   VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
                (turno_id, cajero_id, cliente_id, subtotal, iva, total,
                 metodo_pago_id, monto_recibido, cambio)
            )
            venta_id = cur.fetchone()[0]

            for item in items:
                cur.execute(
                    """INSERT INTO detalle_ventas
                           (venta_id, producto_id, cantidad, precio_unitario, subtotal)
                       VALUES (%s,%s,%s,%s,%s)""",
                    (venta_id, item["producto_id"], item["cantidad"],
                     item["precio"], item["subtotal"])
                )
                cur.execute(
                    """UPDATE inventario
                       SET stock_actual = stock_actual - %s
                       WHERE producto_id = %s""",
                    (item["cantidad"], item["producto_id"])
                )

            cur.execute(
                """UPDATE turnos
                   SET venta_total = venta_total + %s,
                       efectivo_ingresado = efectivo_ingresado + %s,
                       cobro_tarjeta = cobro_tarjeta + %s
                   WHERE id = %s""",
                (total,
                 total if metodo_pago_id == 1 else Decimal("0"),
                 total if metodo_pago_id != 1 else Decimal("0"),
                 turno_id)
            )
        conn.commit()
        return venta_id
    except Exception:
        conn.rollback()
        raise


def find_by_turno(turno_id: int):
    return DB.fetchall(
        """SELECT v.id, v.total, v.subtotal, v.iva, mp.nombre AS metodo,
                  v.creado_en
           FROM ventas v
           JOIN metodos_pago mp ON mp.id = v.metodo_pago_id
           WHERE v.turno_id = %s
           ORDER BY v.creado_en DESC""",
        (turno_id,)
    )


def find_detalle(venta_id: int):
    return DB.fetchall(
        """SELECT p.nombre, dv.cantidad, dv.precio_unitario, dv.subtotal
           FROM detalle_ventas dv
           JOIN productos p ON p.id = dv.producto_id
           WHERE dv.venta_id = %s""",
        (venta_id,)
    )


def get_metodos_pago():
    rows = DB.fetchall("SELECT id, nombre FROM metodos_pago ORDER BY id")
    if not rows:
        DB.execute("INSERT INTO metodos_pago (nombre) VALUES ('EFECTIVO') ON CONFLICT DO NOTHING")
        DB.execute("INSERT INTO metodos_pago (nombre) VALUES ('TARJETA') ON CONFLICT DO NOTHING")
        rows = DB.fetchall("SELECT id, nombre FROM metodos_pago ORDER BY id")
    return rows


def resumen_global():
    return DB.fetchone(
        """SELECT COUNT(*) AS total_ventas,
                  COALESCE(SUM(total), 0) AS monto_total
           FROM ventas WHERE estado='COMPLETADA'"""
    )
