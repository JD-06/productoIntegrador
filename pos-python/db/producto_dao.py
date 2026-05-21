from db.connection import DB


def find_all(solo_activos=True):
    cond = "WHERE p.activo = TRUE" if solo_activos else ""
    return DB.fetchall(
        f"""SELECT p.id, p.sku, p.nombre, p.precio, p.activo,
                   c.nombre AS categoria,
                   COALESCE(i.stock_actual, 0) AS stock
            FROM productos p
            LEFT JOIN categorias c ON c.id = p.categoria_id
            LEFT JOIN inventario i ON i.producto_id = p.id
            {cond}
            ORDER BY p.nombre"""
    )


def find_by_categoria(categoria_id: int):
    return DB.fetchall(
        """SELECT p.id, p.sku, p.nombre, p.precio,
                  COALESCE(i.stock_actual, 0) AS stock
           FROM productos p
           LEFT JOIN inventario i ON i.producto_id = p.id
           WHERE p.activo = TRUE AND p.categoria_id = %s
           ORDER BY p.nombre""",
        (categoria_id,)
    )


def search(texto: str):
    q = f"%{texto}%"
    return DB.fetchall(
        """SELECT p.id, p.sku, p.nombre, p.precio,
                  COALESCE(i.stock_actual, 0) AS stock
           FROM productos p
           LEFT JOIN inventario i ON i.producto_id = p.id
           WHERE p.activo = TRUE
             AND (p.nombre ILIKE %s OR p.sku ILIKE %s)
           ORDER BY p.nombre LIMIT 50""",
        (q, q)
    )


def insert(sku, nombre, precio, categoria_id=None):
    row = DB.execute_returning(
        """INSERT INTO productos (sku, nombre, precio, categoria_id)
           VALUES (%s, %s, %s, %s) RETURNING id""",
        (sku, nombre, precio, categoria_id)
    )
    if row:
        DB.execute(
            "INSERT INTO inventario (producto_id, stock_actual) VALUES (%s, 0)",
            (row["id"],)
        )
    return row


def update(prod_id, sku, nombre, precio, categoria_id=None):
    DB.execute(
        """UPDATE productos SET sku=%s, nombre=%s, precio=%s, categoria_id=%s
           WHERE id=%s""",
        (sku, nombre, precio, categoria_id, prod_id)
    )


def toggle_activo(prod_id: int):
    DB.execute(
        "UPDATE productos SET activo = NOT activo WHERE id=%s", (prod_id,)
    )
