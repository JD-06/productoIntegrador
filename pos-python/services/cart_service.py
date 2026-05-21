from decimal import Decimal, ROUND_HALF_UP

IVA = Decimal("0.16")
TWO = Decimal("0.01")


def calcular(items: list) -> dict:
    """items: list of {precio, cantidad}"""
    subtotal = sum(
        Decimal(str(i["precio"])) * Decimal(str(i["cantidad"]))
        for i in items
    ).quantize(TWO, ROUND_HALF_UP)
    iva = (subtotal * IVA).quantize(TWO, ROUND_HALF_UP)
    total = (subtotal + iva).quantize(TWO, ROUND_HALF_UP)
    return {"subtotal": subtotal, "iva": iva, "total": total}


def calcular_cambio(total: Decimal, recibido: Decimal) -> Decimal:
    cambio = recibido - total
    return cambio if cambio >= 0 else Decimal("0")
