import json
from db.connection import DB


def find_all(limit=300):
    return DB.fetchall(
        """SELECT l.id, u.nombre AS usuario, l.accion,
                  l.tabla_afectada, l.registro_id, l.detalle, l.creado_en
           FROM log_auditoria l
           LEFT JOIN usuarios u ON u.id = l.usuario_id
           ORDER BY l.creado_en DESC LIMIT %s""",
        (limit,)
    )


def registrar(accion: str, tabla: str = None, registro_id: int = None,
              detalle: dict = None, usuario_id: int = None):
    try:
        DB.execute(
            """INSERT INTO log_auditoria (usuario_id, accion, tabla_afectada, registro_id, detalle)
               VALUES (%s,%s,%s,%s,%s)""",
            (usuario_id, accion, tabla, registro_id,
             json.dumps(detalle) if detalle else None)
        )
    except Exception:
        pass  # audit failures must never crash operations
