import tkinter as tk
from tkinter import ttk, messagebox, simpledialog
import db.usuario_dao as dao
from ui import theme as T


class UsuariosFrame(tk.Frame):
    def __init__(self, master):
        super().__init__(master, bg=T.BG)
        self._build()
        self._cargar()

    def _build(self):
        top = tk.Frame(self, bg=T.BG)
        top.pack(fill="x", padx=16, pady=12)
        tk.Label(top, text="Usuarios", font=T.FONT_LARGE,
                 bg=T.BG, fg=T.TEXT).pack(side="left")
        bf = tk.Frame(top, bg=T.BG)
        bf.pack(side="right")
        for txt, cmd, color in [("+ Nuevo", self._nuevo, T.PRIMARY),
                                  ("Cambiar PIN", self._cambiar_pin, T.WARNING),
                                  ("Activar/Desactivar", self._toggle, T.SURFACE)]:
            tk.Button(bf, text=txt, command=cmd, bg=color,
                      fg=T.TEXT if color != T.WARNING else T.BG,
                      activebackground=T.BORDER, relief="flat",
                      font=T.FONT_BOLD, cursor="hand2"
                      ).pack(side="left", padx=4, ipady=4, ipadx=10)

        cols = ("ID", "Nombre", "Rol", "Activo", "Creado")
        self.tree = ttk.Treeview(self, columns=cols, show="headings")
        for col, w in zip(cols, [50, 180, 100, 70, 160]):
            self.tree.heading(col, text=col)
            self.tree.column(col, width=w, anchor="center")
        self.tree.pack(fill="both", expand=True, padx=16, pady=8)

    def _cargar(self):
        self.users = dao.find_all()
        for r in self.tree.get_children():
            self.tree.delete(r)
        for u in self.users:
            self.tree.insert("", "end", values=(
                u["id"], u["nombre"], u["rol"],
                "✓" if u["activo"] else "✗",
                str(u["creado_en"])[:16]
            ))

    def _selected(self):
        sel = self.tree.selection()
        if not sel:
            messagebox.showwarning("Sin selección", "Selecciona un usuario.", parent=self)
            return None
        return self.users[self.tree.index(sel[0])]

    def _nuevo(self):
        _UserForm(self, dao.find_roles(), self._cargar)

    def _cambiar_pin(self):
        u = self._selected()
        if not u:
            return
        pin = simpledialog.askstring("Cambiar PIN", f"Nuevo PIN para {u['nombre']}:",
                                      show="*", parent=self)
        if pin and pin.strip():
            dao.cambiar_pin(u["id"], pin.strip())
            messagebox.showinfo("OK", "PIN actualizado.", parent=self)

    def _toggle(self):
        u = self._selected()
        if u and messagebox.askyesno("Confirmar",
                f"¿Cambiar estado de '{u['nombre']}'?", parent=self):
            dao.toggle_activo(u["id"])
            self._cargar()


class _UserForm(tk.Toplevel):
    def __init__(self, master, roles, on_save):
        super().__init__(master)
        self.roles = roles
        self.on_save = on_save
        self.title("Nuevo Usuario")
        self.configure(bg=T.BG)
        self.resizable(False, False)
        self.grab_set()
        self.geometry("340x280")
        self._build()

    def _build(self):
        p = tk.Frame(self, bg=T.BG, padx=24, pady=20)
        p.pack(fill="both", expand=True)
        self.vars = {}
        for label, key, show in [("Nombre", "nombre", ""), ("PIN", "pin", "*")]:
            tk.Label(p, text=label, bg=T.BG, fg=T.TEXT,
                     font=T.FONT_BOLD, anchor="w").pack(fill="x")
            v = tk.StringVar()
            tk.Entry(p, textvariable=v, show=show,
                     bg=T.CARD, fg=T.TEXT, insertbackground=T.TEXT,
                     relief="flat", font=T.FONT_NORMAL
                     ).pack(fill="x", ipady=6, pady=(2, 10))
            self.vars[key] = v

        tk.Label(p, text="Rol", bg=T.BG, fg=T.TEXT,
                 font=T.FONT_BOLD, anchor="w").pack(fill="x")
        self.var_rol = tk.StringVar()
        nombres = [r["nombre"] for r in self.roles]
        cb = ttk.Combobox(p, textvariable=self.var_rol,
                           values=nombres, state="readonly", font=T.FONT_NORMAL)
        cb.pack(fill="x", pady=(2, 12))
        if nombres:
            cb.current(0)

        bf = tk.Frame(p, bg=T.BG)
        bf.pack(fill="x")
        tk.Button(bf, text="Cancelar", command=self.destroy,
                  bg=T.SURFACE, fg=T.TEXT, relief="flat", font=T.FONT_BOLD
                  ).pack(side="left", ipady=6, ipadx=12)
        tk.Button(bf, text="Guardar", command=self._guardar,
                  bg=T.PRIMARY, fg=T.TEXT, relief="flat", font=T.FONT_BOLD
                  ).pack(side="right", ipady=6, ipadx=12)

    def _guardar(self):
        nombre = self.vars["nombre"].get().strip()
        pin    = self.vars["pin"].get().strip()
        rol_n  = self.var_rol.get()
        rol    = next((r for r in self.roles if r["nombre"] == rol_n), None)
        if not nombre or not pin or not rol:
            messagebox.showerror("Error", "Todos los campos son requeridos.", parent=self)
            return
        try:
            dao.insert(nombre, pin, rol["id"])
            self.on_save()
            self.destroy()
        except Exception as ex:
            messagebox.showerror("Error", str(ex), parent=self)
