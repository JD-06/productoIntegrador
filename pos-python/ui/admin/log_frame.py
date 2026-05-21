import tkinter as tk
from tkinter import ttk
import db.log_dao as dao
from ui import theme as T


class LogFrame(tk.Frame):
    def __init__(self, master):
        super().__init__(master, bg=T.BG)
        self._build()
        self._cargar()

    def _build(self):
        top = tk.Frame(self, bg=T.BG)
        top.pack(fill="x", padx=16, pady=12)
        tk.Label(top, text="Log de Auditoría", font=T.FONT_LARGE,
                 bg=T.BG, fg=T.TEXT).pack(side="left")
        tk.Button(top, text="Actualizar", command=self._cargar,
                  bg=T.PRIMARY, fg=T.TEXT, activebackground=T.BORDER,
                  relief="flat", font=T.FONT_BOLD, cursor="hand2"
                  ).pack(side="right", ipady=4, ipadx=12)

        cols = ("ID", "Usuario", "Acción", "Tabla", "Registro", "Fecha")
        self.tree = ttk.Treeview(self, columns=cols, show="headings")
        for col, w in zip(cols, [50, 120, 200, 110, 70, 160]):
            self.tree.heading(col, text=col)
            self.tree.column(col, width=w, anchor="center")
        sb = ttk.Scrollbar(self, orient="vertical", command=self.tree.yview)
        self.tree.configure(yscrollcommand=sb.set)
        self.tree.pack(side="left", fill="both", expand=True, padx=(16, 0), pady=8)
        sb.pack(side="right", fill="y", pady=8, padx=(0, 8))

    def _cargar(self):
        self.logs = dao.find_all()
        for r in self.tree.get_children():
            self.tree.delete(r)
        for l in self.logs:
            self.tree.insert("", "end", values=(
                l["id"], l.get("usuario") or "—", l["accion"],
                l.get("tabla_afectada") or "—",
                l.get("registro_id") or "—",
                str(l["creado_en"])[:16]
            ))
