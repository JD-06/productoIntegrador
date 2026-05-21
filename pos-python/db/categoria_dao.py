from db.connection import DB


def find_all():
    return DB.fetchall(
        """SELECT c.id, c.nombre,
                  COUNT(p.id) AS num_productos
           FROM categorias c
           LEFT JOIN productos p ON p.categoria_id = c.id
           GROUP BY c.id, c.nombre
           ORDER BY c.nombre"""
    )


def insert(nombre: str):
    DB.execute(
        "INSERT INTO categorias (nombre) VALUES (%s) ON CONFLICT (nombre) DO NOTHING",
        (nombre,)
    )


def update(cat_id: int, nombre: str):
    DB.execute("UPDATE categorias SET nombre=%s WHERE id=%s", (nombre, cat_id))


def delete(cat_id: int):
    DB.execute("DELETE FROM categorias WHERE id=%s", (cat_id,))


def find_or_create(nombre: str) -> int:
    row = DB.fetchone("SELECT id FROM categorias WHERE nombre=%s", (nombre,))
    if row:
        return row["id"]
    r = DB.execute_returning(
        "INSERT INTO categorias (nombre) VALUES (%s) RETURNING id", (nombre,)
    )
    return r["id"] if r else None
