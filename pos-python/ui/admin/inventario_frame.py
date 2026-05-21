import tkinter as tk
from tkinter import ttk, messagebox, simpledialog
import db.inventario_dao as dao
from ui import theme as T


class InventarioFrame(tk.Frame):
    def __init__(self, master):
        super().__init__(master, bg=T.BG)
        self._build()
        self._cargar()

    def _build(self):
        top = tk.Frame(self, bg=T.BG)
        top.pack(fill="x", padx=16, pady=12)
        tk.Label(top, text="Inventario", font=T.FONT_LARGE,
                 bg=T.BG, fg=T.TEXT).pack(side="left")
        bf = tk.Frame(top, bg=T.BG)
        bf.pack(side="right")
        for txt, cmd, color in [("Entrada", self._entrada, T.SUCCESS),
                                  ("Salida",  self._salida,  T.DANGER),
                                  ("Bajo stock", self._bajo_stock, T.WARNING)]:
            tk.Button(bf, text=txt, command=cmd, bg=color,
                      fg=T.TEXT if color != T.WARNING else T.BG,
                      activebackground=T.BORDER, relief="flat",
                      font=T.FONT_BOLD, cursor="hand2"
                      ).pack(side="left", padx=4, ipady=4, ipadx=10)

        cols = ("ID", "SKU", "Nombre", "Categoría", "Stock actual", "Stock mín.")
        self.tree = ttk.Treeview(self, columns=cols, show="headings")
        for col, w in zip(cols, [40, 90, 200, 110, 100, 90]):
            self.tree.heading(col, text=col)
            self.tree.column(col, width=w, anchor="center")
        sb = ttk.Scrollbar(self, orient="vertical", command=self.tree.yview)
        self.tree.configure(yscrollcommand=sb.set)
        self.tree.pack(side="left", fill="both", expand=True, padx=(16, 0), pady=8)
        sb.pack(side="right", fill="y", pady=8, padx=(0, 8))

    def _cargar(self):
        self.datos = dao.find_all()
        for r in self.tree.get_children():
            self.tree.delete(r)
        for d in self.datos:
            tag = "low" if d["stock_actual"] <= d["stock_minimo"] else ""
            self.tree.insert("", "end", values=(
                d["producto_id"], d["sku"], d["nombre"],
                d.get("categoria") or "—",
                d["stock_actual"], d["stock_minimo"]
            ), tags=(tag,))
        self.tree.tag_configure("low", foreground=T.DANGER)

    def _selected(self):
        sel = self.tree.selection()
        if not sel:
            messagebox.showwarning("Sin selección", "Selecciona un producto.", parent=self)
            return None
        return self.datos[self.tree.index(sel[0])]

    def _entrada(self):
        d = self._selected()
        if not d:
            return
        qty = simpledialog.askinteger("Entrada", f"Cantidad a ingresar ({d['nombre']}):",
                                       minvalue=1, parent=self)
        if qty:
            dao.actualizar_stock(d["producto_id"], "ENTRADA", qty)
            self._cargar()

    def _salida(self):
        d = self._selected()
        if not d:
            return
        qty = simpledialog.askinteger("Salida", f"Cantidad a retirar ({d['nombre']}):",
                                       minvalue=1, parent=self)
        if qty:
            dao.actualizar_stock(d["producto_id"], "SALIDA", qty)
            self._cargar()

    def _bajo_stock(self):
        bajo = dao.find_bajo_stock()
        win = tk.Toplevel(self)
        win.title("Productos con bajo stock")
        win.configure(bg=T.BG)
        win.geometry("500x300")
        cols = ("Nombre", "Stock actual", "Stock mín.")
        tree = ttk.Treeview(win, columns=cols, show="headings")
        for col, w in zip(cols, [250, 120, 100]):
            tree.heading(col, text=col)
            tree.column(col, width=w, anchor="center")
        for d in bajo:
            tree.insert("", "end", values=(d["nombre"], d["stock_actual"], d["stock_minimo"]))
        tree.pack(fill="both", expand=True, padx=12, pady=12)
