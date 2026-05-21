from db.connection import DB
from decimal import Decimal


def find_cxc():
    return DB.fetchall(
        """SELECT cc.id, c.nombre AS cliente, cc.monto, cc.saldo,
                  cc.estado, cc.vencimiento, cc.creado_en
           FROM cuentas_cobrar cc
           JOIN clientes c ON c.id = cc.cliente_id
           ORDER BY cc.creado_en DESC"""
    )


def insert_cxc(cliente_id, venta_id, monto, vencimiento=None):
    DB.execute(
        """INSERT INTO cuentas_cobrar (cliente_id, venta_id, monto, saldo, vencimiento)
           VALUES (%s,%s,%s,%s,%s)""",
        (cliente_id, venta_id, monto, monto, vencimiento)
    )


def pagar_cxc(cxc_id: int, pago: Decimal):
    DB.execute(
        """UPDATE cuentas_cobrar
           SET saldo = GREATEST(0, saldo - %s),
               estado = CASE WHEN saldo - %s <= 0 THEN 'PAGADA' ELSE 'PENDIENTE' END
           WHERE id=%s""",
        (pago, pago, cxc_id)
    )


def find_cxp():
    return DB.fetchall(
        """SELECT cp.id, p.nombre AS proveedor, cp.monto, cp.saldo,
                  cp.estado, cp.vencimiento, cp.creado_en
           FROM cuentas_pagar cp
           JOIN proveedores p ON p.id = cp.proveedor_id
           ORDER BY cp.creado_en DESC"""
    )


def insert_cxp(proveedor_id, compra_id, monto, vencimiento=None):
    DB.execute(
        """INSERT INTO cuentas_pagar (proveedor_id, compra_id, monto, saldo, vencimiento)
           VALUES (%s,%s,%s,%s,%s)""",
        (proveedor_id, compra_id, monto, monto, vencimiento)
    )


def pagar_cxp(cxp_id: int, pago: Decimal):
    DB.execute(
        """UPDATE cuentas_pagar
           SET saldo = GREATEST(0, saldo - %s),
               estado = CASE WHEN saldo - %s <= 0 THEN 'PAGADA' ELSE 'PENDIENTE' END
           WHERE id=%s""",
        (pago, pago, cxp_id)
    )
