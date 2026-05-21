import tkinter as tk
from tkinter import ttk, messagebox, simpledialog
import db.categoria_dao as dao
from ui import theme as T


class CategoriasFrame(tk.Frame):
    def __init__(self, master):
        super().__init__(master, bg=T.BG)
        self._build()
        self._cargar()

    def _build(self):
        top = tk.Frame(self, bg=T.BG)
        top.pack(fill="x", padx=16, pady=12)
        tk.Label(top, text="Categorías", font=T.FONT_LARGE,
                 bg=T.BG, fg=T.TEXT).pack(side="left")
        bf = tk.Frame(top, bg=T.BG)
        bf.pack(side="right")
        for txt, cmd, color in [("+ Nueva", self._nueva, T.PRIMARY),
                                  ("Editar", self._editar, T.WARNING),
                                  ("Eliminar", self._eliminar, T.DANGER)]:
            tk.Button(bf, text=txt, command=cmd, bg=color,
                      fg=T.TEXT if color != T.WARNING else T.BG,
                      activebackground=T.BORDER, relief="flat",
                      font=T.FONT_BOLD, cursor="hand2"
                      ).pack(side="left", padx=4, ipady=4, ipadx=10)

        cols = ("ID", "Nombre", "Productos")
        self.tree = ttk.Treeview(self, columns=cols, show="headings")
        for col, w in zip(cols, [50, 250, 100]):
            self.tree.heading(col, text=col)
            self.tree.column(col, width=w, anchor="center")
        self.tree.pack(fill="both", expand=True, padx=16, pady=8)

    def _cargar(self):
        self.cats = dao.find_all()
        for r in self.tree.get_children():
            self.tree.delete(r)
        for c in self.cats:
            self.tree.insert("", "end", values=(c["id"], c["nombre"], c["num_productos"]))

    def _selected(self):
        sel = self.tree.selection()
        if not sel:
            messagebox.showwarning("Sin selección", "Selecciona una categoría.", parent=self)
            return None
        return self.cats[self.tree.index(sel[0])]

    def _nueva(self):
        n = simpledialog.askstring("Nueva categoría", "Nombre:", parent=self)
        if n and n.strip():
            dao.insert(n.strip()); self._cargar()

    def _editar(self):
        cat = self._selected()
        if cat:
            n = simpledialog.askstring("Editar", "Nombre:", initialvalue=cat["nombre"], parent=self)
            if n and n.strip():
                dao.update(cat["id"], n.strip()); self._cargar()

    def _eliminar(self):
        cat = self._selected()
        if cat and messagebox.askyesno("Eliminar", f"¿Eliminar '{cat['nombre']}'?", parent=self):
            try:
                dao.delete(cat["id"]); self._cargar()
            except Exception as ex:
                messagebox.showerror("Error", str(ex), parent=self)
