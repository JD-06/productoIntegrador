import tkinter as tk
from tkinter import ttk, messagebox, simpledialog
from decimal import Decimal
import db.cuenta_dao as dao
from ui import theme as T


class CxPFrame(tk.Frame):
    def __init__(self, master):
        super().__init__(master, bg=T.BG)
        self._build()
        self._cargar()

    def _build(self):
        top = tk.Frame(self, bg=T.BG)
        top.pack(fill="x", padx=16, pady=12)
        tk.Label(top, text="Cuentas por Pagar", font=T.FONT_LARGE,
                 bg=T.BG, fg=T.TEXT).pack(side="left")
        tk.Button(top, text="Registrar pago", command=self._pagar,
                  bg=T.SUCCESS, fg=T.TEXT, activebackground=T.BORDER,
                  relief="flat", font=T.FONT_BOLD, cursor="hand2"
                  ).pack(side="right", ipady=4, ipadx=12)

        cols = ("ID", "Proveedor", "Monto", "Saldo", "Estado", "Vencimiento")
        self.tree = ttk.Treeview(self, columns=cols, show="headings")
        for col, w in zip(cols, [50, 200, 90, 90, 90, 110]):
            self.tree.heading(col, text=col)
            self.tree.column(col, width=w, anchor="center")
        self.tree.pack(fill="both", expand=True, padx=16, pady=8)
        self.tree.tag_configure("pagada", foreground=T.MUTED)
        self.tree.tag_configure("pendiente", foreground=T.WARNING)

    def _cargar(self):
        self.cuentas = dao.find_cxp()
        for r in self.tree.get_children():
            self.tree.delete(r)
        for c in self.cuentas:
            tag = "pagada" if c["estado"] == "PAGADA" else "pendiente"
            self.tree.insert("", "end", values=(
                c["id"], c["proveedor"],
                f"${Decimal(str(c['monto'])):.2f}",
                f"${Decimal(str(c['saldo'])):.2f}",
                c["estado"],
                str(c.get("vencimiento") or "—")
            ), tags=(tag,))

    def _pagar(self):
        sel = self.tree.selection()
        if not sel:
            messagebox.showwarning("Sin selección", "Selecciona una cuenta.", parent=self)
            return
        c = self.cuentas[self.tree.index(sel[0])]
        monto = simpledialog.askfloat("Pago", f"Monto a pagar (saldo: ${c['saldo']:.2f}):",
                                       minvalue=0.01, parent=self)
        if monto:
            dao.pagar_cxp(c["id"], Decimal(str(monto)))
            self._cargar()
