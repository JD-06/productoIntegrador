import hashlib
from db.connection import DB


def _sha256(text: str) -> str:
    return hashlib.sha256(text.encode()).hexdigest()


def verificar_pin(nombre: str, pin: str):
    """Returns user dict or None."""
    return DB.fetchone(
        """SELECT u.id, u.nombre, r.nombre AS rol
           FROM usuarios u
           JOIN roles r ON r.id = u.rol_id
           WHERE u.nombre = %s AND u.pin_hash = %s AND u.activo = TRUE""",
        (nombre, _sha256(pin))
    )


def find_all():
    return DB.fetchall(
        """SELECT u.id, u.nombre, r.nombre AS rol, u.activo, u.creado_en
           FROM usuarios u JOIN roles r ON r.id = u.rol_id
           ORDER BY u.nombre"""
    )


def find_roles():
    return DB.fetchall("SELECT id, nombre FROM roles ORDER BY nombre")


def insert(nombre: str, pin: str, rol_id: int):
    DB.execute(
        "INSERT INTO usuarios (nombre, pin_hash, rol_id) VALUES (%s, %s, %s)",
        (nombre, _sha256(pin), rol_id)
    )


def cambiar_pin(usuario_id: int, nuevo_pin: str):
    DB.execute(
        "UPDATE usuarios SET pin_hash = %s WHERE id = %s",
        (_sha256(nuevo_pin), usuario_id)
    )


def toggle_activo(usuario_id: int):
    DB.execute(
        "UPDATE usuarios SET activo = NOT activo WHERE id = %s",
        (usuario_id,)
    )
