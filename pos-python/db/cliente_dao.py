from db.connection import DB


def find_all():
    return DB.fetchall(
        """SELECT id, codigo, nombre, rfc, tipo, puntos_acumulados, creado_en
           FROM clientes ORDER BY nombre"""
    )


def insert(codigo, nombre, rfc, tipo):
    DB.execute(
        "INSERT INTO clientes (codigo, nombre, rfc, tipo) VALUES (%s,%s,%s,%s)",
        (codigo, nombre, rfc, tipo)
    )


def update(cliente_id, nombre, rfc, tipo):
    DB.execute(
        "UPDATE clientes SET nombre=%s, rfc=%s, tipo=%s WHERE id=%s",
        (nombre, rfc, tipo, cliente_id)
    )


def delete(cliente_id: int):
    DB.execute("DELETE FROM clientes WHERE id=%s", (cliente_id,))


def agregar_puntos(cliente_id: int, puntos: int, venta_id: int = None):
    DB.execute(
        "UPDATE clientes SET puntos_acumulados = puntos_acumulados + %s WHERE id=%s",
        (puntos, cliente_id)
    )
    DB.execute(
        """INSERT INTO transacciones_puntos (cliente_id, venta_id, puntos, motivo)
           VALUES (%s, %s, %s, 'VENTA')""",
        (cliente_id, venta_id, puntos)
    )
