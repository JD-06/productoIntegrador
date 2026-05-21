from decimal import Decimal
from db.connection import DB


def find_periodos():
    return DB.fetchall(
        "SELECT id, periodo, creado_en FROM periodos_nomina ORDER BY creado_en DESC"
    )


def find_by_periodo(periodo_id: int):
    return DB.fetchall(
        """SELECT n.id, u.nombre AS empleado, e.puesto,
                  n.salario_base, n.deducciones, n.neto_pagar
           FROM nomina n
           JOIN empleados e ON e.id = n.empleado_id
           JOIN usuarios u ON u.id = e.usuario_id
           WHERE n.periodo_id = %s
           ORDER BY u.nombre""",
        (periodo_id,)
    )


def crear_periodo(periodo: str) -> int:
    row = DB.execute_returning(
        "INSERT INTO periodos_nomina (periodo) VALUES (%s) RETURNING id", (periodo,)
    )
    return row["id"] if row else None


def insertar_linea(periodo_id, empleado_id, salario_base, deducciones):
    neto = Decimal(str(salario_base)) - Decimal(str(deducciones))
    DB.execute(
        """INSERT INTO nomina (periodo_id, empleado_id, salario_base, deducciones, neto_pagar)
           VALUES (%s,%s,%s,%s,%s)""",
        (periodo_id, empleado_id, salario_base, deducciones, neto)
    )
