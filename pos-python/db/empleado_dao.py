from db.connection import DB


def find_all():
    return DB.fetchall(
        """SELECT e.id, u.nombre, e.puesto, e.salario, e.activo
           FROM empleados e
           JOIN usuarios u ON u.id = e.usuario_id
           ORDER BY u.nombre"""
    )


def insert(usuario_id: int, puesto: str, salario):
    DB.execute(
        "INSERT INTO empleados (usuario_id, puesto, salario) VALUES (%s,%s,%s)",
        (usuario_id, puesto, salario)
    )


def update(emp_id: int, puesto: str, salario):
    DB.execute(
        "UPDATE empleados SET puesto=%s, salario=%s WHERE id=%s",
        (puesto, salario, emp_id)
    )


def delete(emp_id: int):
    DB.execute("DELETE FROM empleados WHERE id=%s", (emp_id,))
