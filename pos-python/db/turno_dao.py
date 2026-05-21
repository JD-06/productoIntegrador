from decimal import Decimal
from db.connection import DB


def abrir(cajero_id: int, fondo: Decimal) -> dict:
    codigo = _siguiente_codigo()
    return DB.execute_returning(
        """INSERT INTO turnos (codigo, cajero_id, fondo_inicial)
           VALUES (%s, %s, %s) RETURNING id, codigo""",
        (codigo, cajero_id, fondo)
    )


def cerrar(turno_id: int):
    DB.execute(
        "UPDATE turnos SET estado='CERRADO', cerrado_en=NOW() WHERE id=%s",
        (turno_id,)
    )


def find_activos():
    return DB.fetchall(
        """SELECT t.id, t.codigo, u.nombre AS cajero, t.fondo_inicial,
                  t.venta_total, t.abierto_en
           FROM turnos t JOIN usuarios u ON u.id = t.cajero_id
           WHERE t.estado='ABIERTO' ORDER BY t.abierto_en DESC"""
    )


def find_all():
    return DB.fetchall(
        """SELECT t.id, t.codigo, u.nombre AS cajero, t.fondo_inicial,
                  t.venta_total, t.estado, t.abierto_en, t.cerrado_en
           FROM turnos t JOIN usuarios u ON u.id = t.cajero_id
           ORDER BY t.abierto_en DESC LIMIT 200"""
    )


def _siguiente_codigo() -> str:
    row = DB.fetchone("SELECT COUNT(*) AS cnt FROM turnos")
    n = (row["cnt"] if row else 0) + 1
    return f"T-{n:05d}"
