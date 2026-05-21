import tkinter as tk
from tkinter import ttk, messagebox
from decimal import Decimal
import db.compra_dao as dao
import db.producto_dao as pdao
from ui import theme as T


class ComprasFrame(tk.Frame):
    def __init__(self, master):
        super().__init__(master, bg=T.BG)
        self._build()
        self._cargar()

    def _build(self):
        top = tk.Frame(self, bg=T.BG)
        top.pack(fill="x", padx=16, pady=12)
        tk.Label(top, text="Compras / Inventario", font=T.FONT_LARGE,
                 bg=T.BG, fg=T.TEXT).pack(side="left")
        bf = tk.Frame(top, bg=T.BG)
        bf.pack(side="right")
        tk.Button(bf, text="+ Nueva compra", command=self._nueva,
                  bg=T.PRIMARY, fg=T.TEXT, activebackground=T.BORDER,
                  relief="flat", font=T.FONT_BOLD, cursor="hand2"
                  ).pack(side="left", padx=4, ipady=4, ipadx=10)
        tk.Button(bf, text="+ Proveedor", command=self._nuevo_proveedor,
                  bg=T.SURFACE, fg=T.TEXT, activebackground=T.BORDER,
                  relief="flat", font=T.FONT_BOLD, cursor="hand2"
                  ).pack(side="left", padx=4, ipady=4, ipadx=10)

        cols = ("ID", "Proveedor", "Total", "Usuario", "Fecha")
        self.tree = ttk.Treeview(self, columns=cols, show="headings")
        for col, w in zip(cols, [50, 200, 100, 130, 160]):
            self.tree.heading(col, text=col)
            self.tree.column(col, width=w, anchor="center")
        self.tree.pack(fill="both", expand=True, padx=16, pady=8)

    def _cargar(self):
        self.compras = dao.find_all()
        for r in self.tree.get_children():
            self.tree.delete(r)
        for c in self.compras:
            self.tree.insert("", "end", values=(
                c["id"], c.get("proveedor") or "—",
                f"${Decimal(str(c['total'])):.2f}",
                c.get("usuario") or "—",
                str(c["creado_en"])[:16]
            ))

    def _nueva(self):
        _CompraForm(self, self._cargar)

    def _nuevo_proveedor(self):
        win = tk.Toplevel(self)
        win.title("Nuevo Proveedor")
        win.configure(bg=T.BG)
        win.resizable(False, False)
        win.grab_set()
        win.geometry("340x240")
        p = tk.Frame(win, bg=T.BG, padx=24, pady=20)
        p.pack(fill="both", expand=True)
        vars_ = {}
        for lbl, key in [("Nombre", "nombre"), ("RFC", "rfc"), ("Contacto", "contacto")]:
            tk.Label(p, text=lbl, bg=T.BG, fg=T.TEXT, font=T.FONT_BOLD, anchor="w").pack(fill="x")
            v = tk.StringVar()
            tk.Entry(p, textvariable=v, bg=T.CARD, fg=T.TEXT, insertbackground=T.TEXT,
                     relief="flat", font=T.FONT_NORMAL).pack(fill="x", ipady=6, pady=(2, 8))
            vars_[key] = v

        def _save():
            try:
                dao.insert_proveedor(vars_["nombre"].get(), vars_["rfc"].get(), vars_["contacto"].get())
                win.destroy()
            except Exception as ex:
                messagebox.showerror("Error", str(ex), parent=win)

        tk.Button(p, text="Guardar", command=_save,
                  bg=T.PRIMARY, fg=T.TEXT, relief="flat", font=T.FONT_BOLD
                  ).pack(fill="x", ipady=6)


class _CompraForm(tk.Toplevel):
    def __init__(self, master, on_save):
        super().__init__(master)
        self.on_save = on_save
        self.title("Nueva Compra")
        self.configure(bg=T.BG)
        self.geometry("600x500")
        self.grab_set()
        self.items = []
        self.proveedores = dao.find_proveedores()
        self.productos   = pdao.find_all()
        self._build()

    def _build(self):
        p = tk.Frame(self, bg=T.BG, padx=20, pady=16)
        p.pack(fill="both", expand=True)

        # Proveedor
        tf = tk.Frame(p, bg=T.BG)
        tf.pack(fill="x", pady=(0, 12))
        tk.Label(tf, text="Proveedor:", bg=T.BG, fg=T.TEXT, font=T.FONT_BOLD).pack(side="left")
        self.var_prov = tk.StringVar()
        ttk.Combobox(tf, textvariable=self.var_prov,
                     values=[pr["nombre"] for pr in self.proveedores],
                     state="readonly", font=T.FONT_NORMAL
                     ).pack(side="left", padx=8)

        # Add item
        af = tk.Frame(p, bg=T.CARD, padx=12, pady=10)
        af.pack(fill="x", pady=(0, 12))
        tk.Label(af, text="Producto:", bg=T.CARD, fg=T.TEXT).pack(side="left")
        self.var_prod = tk.StringVar()
        ttk.Combobox(af, textvariable=self.var_prod,
                     values=[pr["nombre"] for pr in self.productos],
                     font=T.FONT_NORMAL, width=22
                     ).pack(side="left", padx=6)
        tk.Label(af, text="Cant:", bg=T.CARD, fg=T.TEXT).pack(side="left")
        self.var_qty = tk.StringVar(value="1")
        tk.Entry(af, textvariable=self.var_qty, bg=T.BG, fg=T.TEXT,
                 insertbackground=T.TEXT, width=6, relief="flat"
                 ).pack(side="left", padx=4)
        tk.Label(af, text="Costo:", bg=T.CARD, fg=T.TEXT).pack(side="left")
        self.var_costo = tk.StringVar(value="0.00")
        tk.Entry(af, textvariable=self.var_costo, bg=T.BG, fg=T.TEXT,
                 insertbackground=T.TEXT, width=8, relief="flat"
                 ).pack(side="left", padx=4)
        tk.Button(af, text="+ Agregar", command=self._agregar_item,
                  bg=T.PRIMARY, fg=T.TEXT, relief="flat", font=T.FONT_BOLD
                  ).pack(side="left", padx=6, ipady=4, ipadx=8)

        # Items list
        cols = ("Producto", "Cantidad", "Costo unit.", "Subtotal")
        self.tree = ttk.Treeview(p, columns=cols, show="headings", height=8)
        for col, w in zip(cols, [220, 80, 100, 100]):
            self.tree.heading(col, text=col)
            self.tree.column(col, width=w, anchor="center")
        self.tree.pack(fill="x", pady=8)

        self.lbl_total = tk.Label(p, text="Total: $0.00",
                                   bg=T.BG, fg=T.SUCCESS,
                                   font=("Segoe UI", 14, "bold"))
        self.lbl_total.pack(anchor="e", pady=4)

        bf = tk.Frame(p, bg=T.BG)
        bf.pack(fill="x")
        tk.Button(bf, text="Cancelar", command=self.destroy,
                  bg=T.SURFACE, fg=T.TEXT, relief="flat", font=T.FONT_BOLD
                  ).pack(side="left", ipady=6, ipadx=12)
        tk.Button(bf, text="Registrar compra", command=self._guardar,
                  bg=T.SUCCESS, fg=T.TEXT, relief="flat", font=T.FONT_BOLD
                  ).pack(side="right", ipady=6, ipadx=12)

    def _agregar_item(self):
        pname = self.var_prod.get()
        prod = next((p for p in self.productos if p["nombre"] == pname), None)
        if not prod:
            messagebox.showwarning("Sin producto", "Selecciona un producto.", parent=self)
            return
        try:
            qty  = int(self.var_qty.get())
            cost = Decimal(self.var_costo.get())
        except Exception:
            messagebox.showerror("Error", "Cantidad/costo inválidos.", parent=self)
            return
        sub = qty * cost
        self.items.append({"producto_id": prod["id"], "nombre": prod["nombre"],
                            "cantidad": qty, "costo_unitario": cost})
        self.tree.insert("", "end", values=(prod["nombre"], qty, f"${cost:.2f}", f"${sub:.2f}"))
        total = sum(i["cantidad"] * i["costo_unitario"] for i in self.items)
        self.lbl_total.config(text=f"Total: ${total:.2f}")

    def _guardar(self):
        if not self.items:
            messagebox.showwarning("Sin items", "Agrega productos.", parent=self)
            return
        pname = self.var_prov.get()
        prov = next((p for p in self.proveedores if p["nombre"] == pname), None)
        try:
            dao.insertar_compra(prov["id"] if prov else None, None, self.items)
            self.on_save()
            self.destroy()
        except Exception as ex:
            messagebox.showerror("Error", str(ex), parent=self)
