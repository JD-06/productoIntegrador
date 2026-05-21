import tkinter as tk
from tkinter import ttk, messagebox
from decimal import Decimal
import db.empleado_dao as dao
import db.usuario_dao as udao
from ui import theme as T


class EmpleadosFrame(tk.Frame):
    def __init__(self, master):
        super().__init__(master, bg=T.BG)
        self._build()
        self._cargar()

    def _build(self):
        top = tk.Frame(self, bg=T.BG)
        top.pack(fill="x", padx=16, pady=12)
        tk.Label(top, text="Empleados", font=T.FONT_LARGE,
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

        cols = ("ID", "Nombre", "Puesto", "Salario", "Activo")
        self.tree = ttk.Treeview(self, columns=cols, show="headings")
        for col, w in zip(cols, [50, 200, 150, 100, 70]):
            self.tree.heading(col, text=col)
            self.tree.column(col, width=w, anchor="center")
        self.tree.pack(fill="both", expand=True, padx=16, pady=8)

    def _cargar(self):
        self.empleados = dao.find_all()
        for r in self.tree.get_children():
            self.tree.delete(r)
        for e in self.empleados:
            self.tree.insert("", "end", values=(
                e["id"], e["nombre"], e.get("puesto") or "—",
                f"${Decimal(str(e['salario'])):.2f}",
                "✓" if e["activo"] else "✗"
            ))

    def _selected(self):
        sel = self.tree.selection()
        if not sel:
            messagebox.showwarning("Sin selección", "Selecciona un empleado.", parent=self)
            return None
        return self.empleados[self.tree.index(sel[0])]

    def _nuevo(self):
        _EmpForm(self, None, udao.find_all(), self._cargar)

    def _editar(self):
        e = self._selected()
        if e:
            _EmpForm(self, e, udao.find_all(), self._cargar)

    def _eliminar(self):
        e = self._selected()
        if e and messagebox.askyesno("Eliminar",
                f"¿Eliminar empleado '{e['nombre']}'?", parent=self):
            try:
                dao.delete(e["id"]); self._cargar()
            except Exception as ex:
                messagebox.showerror("Error", str(ex), parent=self)


class _EmpForm(tk.Toplevel):
    def __init__(self, master, emp, usuarios, on_save):
        super().__init__(master)
        self.emp = emp
        self.usuarios = usuarios
        self.on_save = on_save
        self.title("Nuevo Empleado" if not emp else "Editar Empleado")
        self.configure(bg=T.BG)
        self.resizable(False, False)
        self.grab_set()
        self.geometry("340x260")
        self._build()

    def _build(self):
        p = tk.Frame(self, bg=T.BG, padx=24, pady=20)
        p.pack(fill="both", expand=True)

        if not self.emp:
            tk.Label(p, text="Usuario", bg=T.BG, fg=T.TEXT,
                     font=T.FONT_BOLD, anchor="w").pack(fill="x")
            self.var_usr = tk.StringVar()
            ttk.Combobox(p, textvariable=self.var_usr,
                         values=[u["nombre"] for u in self.usuarios],
                         state="readonly", font=T.FONT_NORMAL
                         ).pack(fill="x", pady=(2, 10))

        self.vars = {}
        defaults = {"puesto": self.emp.get("puesto") or "" if self.emp else "",
                    "salario": str(self.emp["salario"]) if self.emp else "0.00"}
        for label, key in [("Puesto", "puesto"), ("Salario ($)", "salario")]:
            tk.Label(p, text=label, bg=T.BG, fg=T.TEXT,
                     font=T.FONT_BOLD, anchor="w").pack(fill="x")
            v = tk.StringVar(value=defaults[key])
            tk.Entry(p, textvariable=v, bg=T.CARD, fg=T.TEXT,
                     insertbackground=T.TEXT, relief="flat", font=T.FONT_NORMAL
                     ).pack(fill="x", ipady=6, pady=(2, 10))
            self.vars[key] = v

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
            puesto  = self.vars["puesto"].get().strip()
            salario = Decimal(self.vars["salario"].get())
            if self.emp:
                dao.update(self.emp["id"], puesto, salario)
            else:
                usr_n = self.var_usr.get()
                usr = next((u for u in self.usuarios if u["nombre"] == usr_n), None)
                if not usr:
                    raise ValueError("Selecciona un usuario.")
                dao.insert(usr["id"], puesto, salario)
            self.on_save()
            self.destroy()
        except Exception as ex:
            from tkinter import messagebox
            messagebox.showerror("Error", str(ex), parent=self)
