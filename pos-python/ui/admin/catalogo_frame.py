import tkinter as tk
from tkinter import ttk, messagebox, simpledialog
from decimal import Decimal

import db.producto_dao as pdao
import db.categoria_dao as catdao
from ui import theme as T


class CatalogoFrame(tk.Frame):
    def __init__(self, master):
        super().__init__(master, bg=T.BG)
        self._build()
        self._cargar()

    def _build(self):
        top = tk.Frame(self, bg=T.BG)
        top.pack(fill="x", padx=16, pady=12)
        tk.Label(top, text="Catálogo de Productos", font=T.FONT_LARGE,
                 bg=T.BG, fg=T.TEXT).pack(side="left")

        bf = tk.Frame(top, bg=T.BG)
        bf.pack(side="right")
        for txt, cmd, color in [
            ("+ Nuevo", self._nuevo, T.PRIMARY),
            ("Editar",  self._editar, T.WARNING),
            ("Activar/Inactivar", self._toggle, T.SURFACE),
        ]:
            tk.Button(bf, text=txt, command=cmd,
                      bg=color, fg=T.TEXT if color != T.WARNING else T.BG,
                      activebackground=T.BORDER, relief="flat",
                      font=T.FONT_BOLD, cursor="hand2"
                      ).pack(side="left", padx=4, ipady=4, ipadx=10)

        # Search
        sf = tk.Frame(self, bg=T.BG)
        sf.pack(fill="x", padx=16, pady=(0, 8))
        self.var_q = tk.StringVar()
        self.var_q.trace_add("write", lambda *_: self._filtrar())
        tk.Entry(sf, textvariable=self.var_q,
                 bg=T.CARD, fg=T.TEXT, insertbackground=T.TEXT,
                 relief="flat", font=T.FONT_NORMAL
                 ).pack(side="left", fill="x", expand=True, ipady=6)
        tk.Label(sf, text=" Buscar", bg=T.BG, fg=T.MUTED).pack(side="left")

        # Treeview
        cols = ("ID", "SKU", "Nombre", "Categoría", "Precio", "Stock", "Activo")
        self.tree = ttk.Treeview(self, columns=cols, show="headings",
                                  selectmode="browse")
        widths = [40, 90, 200, 120, 80, 60, 60]
        for col, w in zip(cols, widths):
            self.tree.heading(col, text=col)
            self.tree.column(col, width=w, anchor="center")
        sb = ttk.Scrollbar(self, orient="vertical", command=self.tree.yview)
        self.tree.configure(yscrollcommand=sb.set)
        self.tree.pack(side="left", fill="both", expand=True, padx=(16, 0), pady=8)
        sb.pack(side="right", fill="y", pady=8, padx=(0, 8))

    def _cargar(self):
        self.productos = pdao.find_all(solo_activos=False)
        self._render(self.productos)

    def _render(self, prods):
        for r in self.tree.get_children():
            self.tree.delete(r)
        for p in prods:
            self.tree.insert("", "end", values=(
                p["id"], p["sku"], p["nombre"],
                p.get("categoria") or "—",
                f"${Decimal(str(p['precio'])):.2f}",
                p.get("stock", 0),
                "✓" if p["activo"] else "✗",
            ))

    def _filtrar(self):
        q = self.var_q.get().strip().lower()
        if not q:
            self._render(self.productos)
            return
        filtered = [p for p in self.productos
                    if q in p["nombre"].lower() or q in p["sku"].lower()]
        self._render(filtered)

    def _selected(self):
        sel = self.tree.selection()
        if not sel:
            messagebox.showwarning("Sin selección", "Selecciona un producto.", parent=self)
            return None
        idx = self.tree.index(sel[0])
        return self.productos[idx]

    def _nuevo(self):
        _ProductoForm(self, None, self._cargar)

    def _editar(self):
        p = self._selected()
        if p:
            _ProductoForm(self, p, self._cargar)

    def _toggle(self):
        p = self._selected()
        if p and messagebox.askyesno("Confirmar",
                f"¿Cambiar estado de '{p['nombre']}'?", parent=self):
            pdao.toggle_activo(p["id"])
            self._cargar()


class _ProductoForm(tk.Toplevel):
    def __init__(self, master, producto, on_save):
        super().__init__(master)
        self.producto = producto
        self.on_save = on_save
        self.title("Nuevo Producto" if not producto else "Editar Producto")
        self.configure(bg=T.BG)
        self.resizable(False, False)
        self._build()
        self.grab_set()
        self._center()

    def _center(self):
        self.update_idletasks()
        w, h = 380, 340
        x = (self.winfo_screenwidth() - w) // 2
        y = (self.winfo_screenheight() - h) // 2
        self.geometry(f"{w}x{h}+{x}+{y}")

    def _build(self):
        p = tk.Frame(self, bg=T.BG, padx=24, pady=20)
        p.pack(fill="both", expand=True)

        cats = catdao.find_all()
        self.cat_map = {c["nombre"]: c["id"] for c in cats}

        fields = [("SKU", "sku"), ("Nombre", "nombre"),
                  ("Precio", "precio"), ("Categoría", "cat")]
        self.vars = {}
        for label, key in fields:
            tk.Label(p, text=label, bg=T.BG, fg=T.TEXT,
                     font=T.FONT_BOLD, anchor="w").pack(fill="x")
            if key == "cat":
                v = tk.StringVar()
                cb = ttk.Combobox(p, textvariable=v,
                                   values=list(self.cat_map.keys()),
                                   state="readonly", font=T.FONT_NORMAL)
                cb.pack(fill="x", pady=(2, 10))
                if self.producto and self.producto.get("categoria"):
                    v.set(self.producto["categoria"])
                self.vars[key] = v
            else:
                v = tk.StringVar(value=str(self.producto[key]) if self.producto else "")
                tk.Entry(p, textvariable=v,
                         bg=T.CARD, fg=T.TEXT, insertbackground=T.TEXT,
                         relief="flat", font=T.FONT_NORMAL
                         ).pack(fill="x", ipady=6, pady=(2, 10))
                self.vars[key] = v

        bf = tk.Frame(p, bg=T.BG)
        bf.pack(fill="x", pady=8)
        tk.Button(bf, text="Cancelar", command=self.destroy,
                  bg=T.SURFACE, fg=T.TEXT, relief="flat",
                  font=T.FONT_BOLD).pack(side="left", ipady=6, ipadx=12)
        tk.Button(bf, text="Guardar", command=self._guardar,
                  bg=T.PRIMARY, fg=T.TEXT, relief="flat",
                  font=T.FONT_BOLD).pack(side="right", ipady=6, ipadx=12)

    def _guardar(self):
        try:
            sku    = self.vars["sku"].get().strip()
            nombre = self.vars["nombre"].get().strip()
            precio = Decimal(self.vars["precio"].get().strip())
            cat_n  = self.vars["cat"].get()
            cat_id = self.cat_map.get(cat_n)
            if not sku or not nombre:
                raise ValueError("SKU y nombre son requeridos.")
            if self.producto:
                pdao.update(self.producto["id"], sku, nombre, precio, cat_id)
            else:
                pdao.insert(sku, nombre, precio, cat_id)
            self.on_save()
            self.destroy()
        except Exception as ex:
            messagebox.showerror("Error", str(ex), parent=self)
