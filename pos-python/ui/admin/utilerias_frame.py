import tkinter as tk
from tkinter import ttk, messagebox, filedialog
import db.producto_dao as pdao
import db.cliente_dao as cdao
import db.inventario_dao as idao
from services.ticket_service import exportar_csv
from ui import theme as T


class UtileriasFrame(tk.Frame):
    def __init__(self, master):
        super().__init__(master, bg=T.BG)
        self._build()

    def _build(self):
        tk.Label(self, text="Utilerías", font=T.FONT_LARGE,
                 bg=T.BG, fg=T.TEXT).pack(anchor="w", padx=16, pady=12)

        card = tk.Frame(self, bg=T.CARD, padx=20, pady=16)
        card.pack(fill="x", padx=16, pady=8)
        tk.Label(card, text="Exportar CSV", bg=T.CARD, fg=T.TEXT,
                 font=T.FONT_BOLD).pack(anchor="w", pady=(0, 10))
        bf = tk.Frame(card, bg=T.CARD)
        bf.pack(fill="x")
        for txt, cmd in [("Exportar Productos", self._exp_productos),
                          ("Exportar Clientes", self._exp_clientes),
                          ("Exportar Inventario", self._exp_inventario)]:
            tk.Button(bf, text=txt, command=cmd,
                      bg=T.PRIMARY, fg=T.TEXT, activebackground=T.BORDER,
                      relief="flat", font=T.FONT_BOLD, cursor="hand2"
                      ).pack(side="left", padx=6, ipady=6, ipadx=12)

        card2 = tk.Frame(self, bg=T.CARD, padx=20, pady=16)
        card2.pack(fill="x", padx=16, pady=8)
        tk.Label(card2, text="Información del sistema", bg=T.CARD, fg=T.TEXT,
                 font=T.FONT_BOLD).pack(anchor="w", pady=(0, 8))
        from db.connection import DB
        try:
            row = DB.fetchone("SELECT version()")
            ver = list(row.values())[0] if row else "N/A"
        except Exception:
            ver = "N/A"
        tk.Label(card2, text=f"PostgreSQL: {ver[:60]}",
                 bg=T.CARD, fg=T.MUTED, font=T.FONT_NORMAL).pack(anchor="w")
        tk.Label(card2, text="POS Python v1.0",
                 bg=T.CARD, fg=T.MUTED, font=T.FONT_NORMAL).pack(anchor="w")

    def _exp_productos(self):
        path = filedialog.asksaveasfilename(
            defaultextension=".csv", filetypes=[("CSV", "*.csv")],
            title="Exportar productos")
        if path:
            data = pdao.find_all(solo_activos=False)
            exportar_csv(data, ["id","sku","nombre","categoria","precio","stock","activo"], path)
            messagebox.showinfo("OK", f"Exportado: {path}", parent=self)

    def _exp_clientes(self):
        path = filedialog.asksaveasfilename(
            defaultextension=".csv", filetypes=[("CSV", "*.csv")],
            title="Exportar clientes")
        if path:
            data = cdao.find_all()
            exportar_csv(data, ["id","codigo","nombre","rfc","tipo","puntos_acumulados"], path)
            messagebox.showinfo("OK", f"Exportado: {path}", parent=self)

    def _exp_inventario(self):
        path = filedialog.asksaveasfilename(
            defaultextension=".csv", filetypes=[("CSV", "*.csv")],
            title="Exportar inventario")
        if path:
            data = idao.find_all()
            exportar_csv(data, ["producto_id","sku","nombre","categoria","stock_actual","stock_minimo"], path)
            messagebox.showinfo("OK", f"Exportado: {path}", parent=self)
