import tkinter as tk
from tkinter import ttk
from decimal import Decimal
import db.turno_dao as tdao
import db.venta_dao as vdao
from ui import theme as T


class MonitorFrame(tk.Frame):
    def __init__(self, master):
        super().__init__(master, bg=T.BG)
        self._build()
        self._cargar()

    def _build(self):
        top = tk.Frame(self, bg=T.BG)
        top.pack(fill="x", padx=16, pady=12)
        tk.Label(top, text="Monitor de Turnos y Ventas", font=T.FONT_LARGE,
                 bg=T.BG, fg=T.TEXT).pack(side="left")
        tk.Button(top, text="Actualizar", command=self._cargar,
                  bg=T.PRIMARY, fg=T.TEXT, activebackground=T.BORDER,
                  relief="flat", font=T.FONT_BOLD, cursor="hand2"
                  ).pack(side="right", ipady=4, ipadx=12)

        # KPI cards
        kf = tk.Frame(self, bg=T.BG)
        kf.pack(fill="x", padx=16, pady=(0, 12))
        self.kpi_frames = {}
        for key, label in [("ventas", "Total ventas"), ("monto", "Monto total"),
                            ("turnos", "Turnos activos")]:
            card = tk.Frame(kf, bg=T.CARD, padx=20, pady=12)
            card.pack(side="left", padx=8)
            tk.Label(card, text=label, bg=T.CARD, fg=T.MUTED, font=T.FONT_NORMAL).pack()
            lbl = tk.Label(card, text="—", bg=T.CARD, fg=T.SUCCESS,
                           font=("Segoe UI", 18, "bold"))
            lbl.pack()
            self.kpi_frames[key] = lbl

        # Turnos activos
        tk.Label(self, text="Turnos activos", bg=T.BG, fg=T.TEXT,
                 font=T.FONT_BOLD).pack(anchor="w", padx=16)
        cols = ("Código", "Cajero", "Fondo inicial", "Venta total", "Abierto")
        self.tree_turnos = ttk.Treeview(self, columns=cols, show="headings", height=6)
        for col, w in zip(cols, [90, 150, 110, 110, 160]):
            self.tree_turnos.heading(col, text=col)
            self.tree_turnos.column(col, width=w, anchor="center")
        self.tree_turnos.pack(fill="x", padx=16, pady=8)

        # Últimas ventas
        tk.Label(self, text="Ventas recientes", bg=T.BG, fg=T.TEXT,
                 font=T.FONT_BOLD).pack(anchor="w", padx=16)
        cols2 = ("Folio", "Total", "Subtotal", "IVA", "Método", "Hora")
        self.tree_ventas = ttk.Treeview(self, columns=cols2, show="headings", height=8)
        for col, w in zip(cols2, [60, 90, 90, 70, 90, 140]):
            self.tree_ventas.heading(col, text=col)
            self.tree_ventas.column(col, width=w, anchor="center")
        self.tree_ventas.pack(fill="both", expand=True, padx=16, pady=8)

    def _cargar(self):
        resumen = vdao.resumen_global()
        if resumen:
            self.kpi_frames["ventas"].config(text=str(resumen["total_ventas"]))
            self.kpi_frames["monto"].config(
                text=f"${Decimal(str(resumen['monto_total'])):.2f}"
            )

        turnos = tdao.find_activos()
        self.kpi_frames["turnos"].config(text=str(len(turnos)))
        for r in self.tree_turnos.get_children():
            self.tree_turnos.delete(r)
        for t in turnos:
            self.tree_turnos.insert("", "end", values=(
                t["codigo"], t["cajero"],
                f"${Decimal(str(t['fondo_inicial'])):.2f}",
                f"${Decimal(str(t['venta_total'] or 0)):.2f}",
                str(t["abierto_en"])[:16]
            ))

        for r in self.tree_ventas.get_children():
            self.tree_ventas.delete(r)
        # show last 50 ventas across all active turnos
        from db.connection import DB
        ventas = DB.fetchall(
            """SELECT v.id, v.total, v.subtotal, v.iva, mp.nombre AS metodo, v.creado_en
               FROM ventas v JOIN metodos_pago mp ON mp.id = v.metodo_pago_id
               ORDER BY v.creado_en DESC LIMIT 50"""
        )
        for v in ventas:
            self.tree_ventas.insert("", "end", values=(
                f"#{v['id']:06d}",
                f"${Decimal(str(v['total'])):.2f}",
                f"${Decimal(str(v['subtotal'])):.2f}",
                f"${Decimal(str(v['iva'])):.2f}",
                v["metodo"],
                str(v["creado_en"])[:16]
            ))
