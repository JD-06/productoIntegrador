import csv
import os
from datetime import datetime
from decimal import Decimal
from pathlib import Path


def generar_ticket_txt(venta_id, cajero, items, subtotal, iva, total,
                       metodo, recibido, cambio) -> str:
    """Returns ticket as string and saves to /tmp/ticket_{id}.txt"""
    lines = [
        "=" * 40,
        "       POS EMPRESARIAL ERP",
        "=" * 40,
        f"Folio: {venta_id:06d}",
        f"Fecha: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
        f"Cajero: {cajero}",
        "-" * 40,
    ]
    for item in items:
        name = item["nombre"][:22]
        qty  = item["cantidad"]
        price = Decimal(str(item["precio_unitario"]))
        sub   = Decimal(str(item["subtotal"]))
        lines.append(f"{name:<22} {qty:>3}x ${price:>7.2f}")
        lines.append(f"{'':>26} ${sub:>7.2f}")
    lines += [
        "-" * 40,
        f"{'Subtotal:':>28} ${subtotal:>7.2f}",
        f"{'IVA (16%):':>28} ${iva:>7.2f}",
        f"{'TOTAL:':>28} ${total:>7.2f}",
        "-" * 40,
        f"{'Método:':>28} {metodo}",
        f"{'Recibido:':>28} ${recibido:>7.2f}",
        f"{'Cambio:':>28} ${cambio:>7.2f}",
        "=" * 40,
        "      ¡Gracias por su compra!",
        "=" * 40,
    ]
    ticket = "\n".join(lines)
    path = Path(f"/tmp/ticket_{venta_id}.txt")
    path.write_text(ticket, encoding="utf-8")
    return ticket


def exportar_csv(datos: list, campos: list, filepath: str):
    with open(filepath, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=campos, extrasaction="ignore")
        w.writeheader()
        w.writerows(datos)
