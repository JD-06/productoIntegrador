import tkinter as tk
from tkinter import ttk, messagebox, simpledialog
from decimal import Decimal
import db.nomina_dao as dao
import db.empleado_dao as edao
from ui import theme as T


class NominaFrame(tk.Frame):
    def __init__(self, master):
        super().__init__(master, bg=T.BG)
        self._build()
        self._cargar_periodos()

    def _build(self):
        top = tk.Frame(self, bg=T.BG)
        top.pack(fill="x", padx=16, pady=12)
        tk.Label(top, text="Nómina", font=T.FONT_LARGE,
                 bg=T.BG, fg=T.TEXT).pack(side="left")
        bf = tk.Frame(top, bg=T.BG)
        bf.pack(side="right")
        tk.Button(bf, text="+ Período", command=self._nuevo_periodo,
                  bg=T.PRIMARY, fg=T.TEXT, relief="flat", font=T.FONT_BOLD,
                  cursor="hand2").pack(side="left", padx=4, ipady=4, ipadx=10)
        tk.Button(bf, text="+ Línea", command=self._nueva_linea,
                  bg=T.SUCCESS, fg=T.TEXT, relief="flat", font=T.FONT_BOLD,
                  cursor="hand2").pack(side="left", padx=4, ipady=4, ipadx=10)

        body = tk.Frame(self, bg=T.BG)
        body.pack(fill="both", expand=True, padx=16, pady=8)

        # Left: periodos
        lf = tk.Frame(body, bg=T.SURFACE, width=200)
        lf.pack(side="left", fill="y", padx=(0, 8))
        lf.pack_propagate(False)
        tk.Label(lf, text="Períodos", bg=T.SURFACE, fg=T.TEXT,
                 font=T.FONT_BOLD).pack(pady=8)
        self.lst_periodos = tk.Listbox(lf, bg=T.CARD, fg=T.TEXT,
                                        selectbackground=T.PRIMARY,
                                        relief="flat", font=T.FONT_NORMAL,
                                        activestyle="none")
        self.lst_periodos.pack(fill="both", expand=True, padx=8, pady=8)
        self.lst_periodos.bind("<<ListboxSelect>>", self._on_periodo_sel)

        # Right: lineas
        rf = tk.Frame(body, bg=T.BG)
        rf.pack(side="right", fill="both", expand=True)
        cols = ("Empleado", "Puesto", "Salario base", "Deducciones", "Neto")
        self.tree = ttk.Treeview(rf, columns=cols, show="headings")
        for col, w in zip(cols, [160, 120, 110, 100, 100]):
            self.tree.heading(col, text=col)
            self.tree.column(col, width=w, anchor="center")
        self.tree.pack(fill="both", expand=True)

    def _cargar_periodos(self):
        self.periodos = dao.find_periodos()
        self.lst_periodos.delete(0, "end")
        for p in self.periodos:
            self.lst_periodos.insert("end", p["periodo"])

    def _on_periodo_sel(self, _):
        sel = self.lst_periodos.curselection()
        if not sel:
            return
        pid = self.periodos[sel[0]]["id"]
        lineas = dao.find_by_periodo(pid)
        for r in self.tree.get_children():
            self.tree.delete(r)
        for l in lineas:
            self.tree.insert("", "end", values=(
                l["empleado"], l.get("puesto") or "—",
                f"${Decimal(str(l['salario_base'])):.2f}",
                f"${Decimal(str(l['deducciones'])):.2f}",
                f"${Decimal(str(l['neto_pagar'])):.2f}",
            ))

    def _nuevo_periodo(self):
        n = simpledialog.askstring("Período", "Nombre del período (ej. 2024-01):", parent=self)
        if n and n.strip():
            dao.crear_periodo(n.strip())
            self._cargar_periodos()

    def _nueva_linea(self):
        sel = self.lst_periodos.curselection()
        if not sel:
            messagebox.showwarning("Sin período", "Selecciona un período primero.", parent=self)
            return
        periodo_id = self.periodos[sel[0]]["id"]
        empleados  = edao.find_all()
        if not empleados:
            messagebox.showinfo("Sin empleados", "No hay empleados registrados.", parent=self)
            return
        _LineaForm(self, periodo_id, empleados,
                   lambda: self._on_periodo_sel(None))


class _LineaForm(tk.Toplevel):
    def __init__(self, master, periodo_id, empleados, on_save):
        super().__init__(master)
        self.periodo_id = periodo_id
        self.empleados  = empleados
        self.on_save    = on_save
        self.title("Nueva Línea de Nómina")
        self.configure(bg=T.BG)
        self.resizable(False, False)
        self.grab_set()
        self.geometry("360x260")
        self._build()

    def _build(self):
        p = tk.Frame(self, bg=T.BG, padx=24, pady=20)
        p.pack(fill="both", expand=True)

        tk.Label(p, text="Empleado", bg=T.BG, fg=T.TEXT,
                 font=T.FONT_BOLD, anchor="w").pack(fill="x")
        self.var_emp = tk.StringVar()
        ttk.Combobox(p, textvariable=self.var_emp,
                     values=[e["nombre"] for e in self.empleados],
                     state="readonly", font=T.FONT_NORMAL
                     ).pack(fill="x", pady=(2, 10))

        self.vars = {}
        for label, key, default_key in [
            ("Salario base ($)", "salario", "salario"),
            ("Deducciones ($)", "deducciones", None),
        ]:
            tk.Label(p, text=label, bg=T.BG, fg=T.TEXT,
                     font=T.FONT_BOLD, anchor="w").pack(fill="x")
            v = tk.StringVar(value="0.00")
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

        self.var_emp.trace_add("write", self._autoset_salario)

    def _autoset_salario(self, *_):
        n = self.var_emp.get()
        emp = next((e for e in self.empleados if e["nombre"] == n), None)
        if emp:
            self.vars["salario"].set(str(emp["salario"]))

    def _guardar(self):
        n = self.var_emp.get()
        emp = next((e for e in self.empleados if e["nombre"] == n), None)
        if not emp:
            messagebox.showerror("Error", "Selecciona un empleado.", parent=self)
            return
        try:
            sal = Decimal(self.vars["salario"].get())
            ded = Decimal(self.vars["deducciones"].get())
            dao.insertar_linea(self.periodo_id, emp["id"], sal, ded)
            self.on_save()
            self.destroy()
        except Exception as ex:
            messagebox.showerror("Error", str(ex), parent=self)
