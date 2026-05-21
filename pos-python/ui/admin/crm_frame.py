import tkinter as tk
from tkinter import ttk, messagebox, simpledialog
import db.cliente_dao as dao
from ui import theme as T


class CRMFrame(tk.Frame):
    def __init__(self, master):
        super().__init__(master, bg=T.BG)
        self._build()
        self._cargar()

    def _build(self):
        top = tk.Frame(self, bg=T.BG)
        top.pack(fill="x", padx=16, pady=12)
        tk.Label(top, text="Directorio CRM / Clientes", font=T.FONT_LARGE,
                 bg=T.BG, fg=T.TEXT).pack(side="left")
        bf = tk.Frame(top, bg=T.BG)
        bf.pack(side="right")
        for txt, cmd, color in [("+ Nuevo", self._nuevo, T.PRIMARY),
                                  ("Editar", self._editar, T.WARNING),
                                  ("Eliminar", self._eliminar, T.DANGER)]:
            tk.Button(bf, text=txt, command=cmd, bg=color,
                      fg=T.TEXT if color != T.WARNING else T.BG,
                      activebackground=T.BORDER, relief="flat",
                      font=T.FONT_BOLD, cursor="hand2"
                      ).pack(side="left", padx=4, ipady=4, ipadx=10)

        cols = ("ID", "Código", "Nombre", "RFC", "Tipo", "Puntos", "Fecha")
        self.tree = ttk.Treeview(self, columns=cols, show="headings")
        for col, w in zip(cols, [40, 90, 180, 100, 80, 60, 140]):
            self.tree.heading(col, text=col)
            self.tree.column(col, width=w, anchor="center")
        sb = ttk.Scrollbar(self, orient="vertical", command=self.tree.yview)
        self.tree.configure(yscrollcommand=sb.set)
        self.tree.pack(side="left", fill="both", expand=True, padx=(16, 0), pady=8)
        sb.pack(side="right", fill="y", pady=8, padx=(0, 8))

    def _cargar(self):
        self.clientes = dao.find_all()
        for r in self.tree.get_children():
            self.tree.delete(r)
        for c in self.clientes:
            self.tree.insert("", "end", values=(
                c["id"], c["codigo"], c["nombre"],
                c.get("rfc") or "—", c["tipo"],
                c["puntos_acumulados"], str(c["creado_en"])[:10]
            ))

    def _selected(self):
        sel = self.tree.selection()
        if not sel:
            messagebox.showwarning("Sin selección", "Selecciona un cliente.", parent=self)
            return None
        return self.clientes[self.tree.index(sel[0])]

    def _nuevo(self):
        _ClienteForm(self, None, self._cargar)

    def _editar(self):
        c = self._selected()
        if c:
            _ClienteForm(self, c, self._cargar)

    def _eliminar(self):
        c = self._selected()
        if c and messagebox.askyesno("Eliminar", f"¿Eliminar a '{c['nombre']}'?", parent=self):
            try:
                dao.delete(c["id"]); self._cargar()
            except Exception as ex:
                messagebox.showerror("Error", str(ex), parent=self)


class _ClienteForm(tk.Toplevel):
    def __init__(self, master, cliente, on_save):
        super().__init__(master)
        self.cliente = cliente
        self.on_save = on_save
        self.title("Nuevo Cliente" if not cliente else "Editar Cliente")
        self.configure(bg=T.BG)
        self.resizable(False, False)
        self.grab_set()
        self.geometry("360x300")
        self._build()

    def _build(self):
        p = tk.Frame(self, bg=T.BG, padx=24, pady=20)
        p.pack(fill="both", expand=True)
        self.vars = {}
        defaults = {
            "codigo": self.cliente["codigo"] if self.cliente else "",
            "nombre": self.cliente["nombre"] if self.cliente else "",
            "rfc":    self.cliente.get("rfc") or "" if self.cliente else "",
            "tipo":   self.cliente["tipo"] if self.cliente else "PUBLICO",
        }
        for label, key in [("Código", "codigo"), ("Nombre", "nombre"),
                            ("RFC", "rfc")]:
            tk.Label(p, text=label, bg=T.BG, fg=T.TEXT,
                     font=T.FONT_BOLD, anchor="w").pack(fill="x")
            v = tk.StringVar(value=defaults[key])
            tk.Entry(p, textvariable=v, bg=T.CARD, fg=T.TEXT,
                     insertbackground=T.TEXT, relief="flat", font=T.FONT_NORMAL
                     ).pack(fill="x", ipady=6, pady=(2, 8))
            self.vars[key] = v

        tk.Label(p, text="Tipo", bg=T.BG, fg=T.TEXT,
                 font=T.FONT_BOLD, anchor="w").pack(fill="x")
        self.var_tipo = tk.StringVar(value=defaults["tipo"])
        ttk.Combobox(p, textvariable=self.var_tipo,
                     values=["PUBLICO", "EMPRESA", "VIP"],
                     state="readonly", font=T.FONT_NORMAL
                     ).pack(fill="x", pady=(2, 12))

        bf = tk.Frame(p, bg=T.BG)
        bf.pack(fill="x")
        tk.Button(bf, text="Cancelar", command=self.destroy,
                  bg=T.SURFACE, fg=T.TEXT, relief="flat", font=T.FONT_BOLD
                  ).pack(side="left", ipady=6, ipadx=12)
        tk.Button(bf, text="Guardar", command=self._guardar,
                  bg=T.PRIMARY, fg=T.TEXT, relief="flat", font=T.FONT_BOLD
                  ).pack(side="right", ipady=6, ipadx=12)

    def _guardar(self):
        try:
            if self.cliente:
                dao.update(self.cliente["id"], self.vars["nombre"].get(),
                           self.vars["rfc"].get(), self.var_tipo.get())
            else:
                dao.insert(self.vars["codigo"].get(), self.vars["nombre"].get(),
                           self.vars["rfc"].get(), self.var_tipo.get())
            self.on_save()
            self.destroy()
        except Exception as ex:
            messagebox.showerror("Error", str(ex), parent=self)
