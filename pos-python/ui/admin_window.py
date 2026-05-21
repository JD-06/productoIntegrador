import tkinter as tk
from tkinter import ttk
from ui import theme as T


class AdminWindow(tk.Toplevel):
    def __init__(self, master, usuario):
        super().__init__(master)
        self.usuario = usuario
        self.title("Panel de Administración")
        self.configure(bg=T.BG)
        self.geometry("1100x700")
        self._build()
        self._apply_ttk_style()

    def _apply_ttk_style(self):
        from ui.theme import apply_ttk_style
        apply_ttk_style(ttk.Style(self))

    def _build(self):
        # Header
        h = tk.Frame(self, bg=T.HEADER_BG, pady=10, padx=16)
        h.pack(fill="x")
        tk.Label(h, text="Panel de Administración", font=T.FONT_LARGE,
                 bg=T.HEADER_BG, fg=T.TEXT).pack(side="left")
        tk.Label(h, text=f"  {self.usuario['nombre']}",
                 bg=T.HEADER_BG, fg=T.MUTED, font=T.FONT_NORMAL).pack(side="left")
        tk.Button(h, text="Cerrar", command=self.destroy,
                  bg=T.SURFACE, fg=T.TEXT, activebackground=T.CARD,
                  relief="flat", font=T.FONT_BOLD, cursor="hand2"
                  ).pack(side="right", ipady=4, ipadx=12)

        # Sidebar + content
        body = tk.Frame(self, bg=T.BG)
        body.pack(fill="both", expand=True)

        # Sidebar
        sidebar = tk.Frame(body, bg=T.SURFACE, width=180)
        sidebar.pack(side="left", fill="y")
        sidebar.pack_propagate(False)

        self.content = tk.Frame(body, bg=T.BG)
        self.content.pack(side="right", fill="both", expand=True)

        self._current_frame = None
        modules = [
            ("Monitor",      self._show_monitor),
            ("Catálogo",     self._show_catalogo),
            ("Categorías",   self._show_categorias),
            ("Inventario",   self._show_inventario),
            ("Compras",      self._show_compras),
            ("CRM Clientes", self._show_crm),
            ("Cuentas x Cobrar", self._show_cxc),
            ("Cuentas x Pagar",  self._show_cxp),
            ("Nómina",       self._show_nomina),
            ("Empleados",    self._show_empleados),
            ("Usuarios",     self._show_usuarios),
            ("Log Auditoría",self._show_log),
            ("Utilerías",    self._show_utilerias),
        ]
        tk.Label(sidebar, text="Módulos", bg=T.SURFACE, fg=T.MUTED,
                 font=T.FONT_BOLD, pady=8).pack(fill="x")
        for name, cmd in modules:
            btn = tk.Button(sidebar, text=name, command=cmd,
                            bg=T.SURFACE, fg=T.TEXT,
                            activebackground=T.PRIMARY, activeforeground=T.TEXT,
                            relief="flat", font=T.FONT_NORMAL, cursor="hand2",
                            anchor="w", padx=16, pady=8)
            btn.pack(fill="x")
            btn.bind("<Enter>", lambda e, b=btn: b.config(bg=T.CARD))
            btn.bind("<Leave>", lambda e, b=btn: b.config(bg=T.SURFACE))

        self._show_monitor()

    def _show(self, FrameClass):
        if self._current_frame:
            self._current_frame.destroy()
        self._current_frame = FrameClass(self.content)
        self._current_frame.pack(fill="both", expand=True)

    def _show_monitor(self):
        from ui.admin.monitor_frame import MonitorFrame
        self._show(MonitorFrame)

    def _show_catalogo(self):
        from ui.admin.catalogo_frame import CatalogoFrame
        self._show(CatalogoFrame)

    def _show_categorias(self):
        from ui.admin.categorias_frame import CategoriasFrame
        self._show(CategoriasFrame)

    def _show_inventario(self):
        from ui.admin.inventario_frame import InventarioFrame
        self._show(InventarioFrame)

    def _show_compras(self):
        from ui.admin.compras_frame import ComprasFrame
        self._show(ComprasFrame)

    def _show_crm(self):
        from ui.admin.crm_frame import CRMFrame
        self._show(CRMFrame)

    def _show_cxc(self):
        from ui.admin.cxc_frame import CxCFrame
        self._show(CxCFrame)

    def _show_cxp(self):
        from ui.admin.cxp_frame import CxPFrame
        self._show(CxPFrame)

    def _show_nomina(self):
        from ui.admin.nomina_frame import NominaFrame
        self._show(NominaFrame)

    def _show_empleados(self):
        from ui.admin.empleados_frame import EmpleadosFrame
        self._show(EmpleadosFrame)

    def _show_usuarios(self):
        from ui.admin.usuarios_frame import UsuariosFrame
        self._show(UsuariosFrame)

    def _show_log(self):
        from ui.admin.log_frame import LogFrame
        self._show(LogFrame)

    def _show_utilerias(self):
        from ui.admin.utilerias_frame import UtileriasFrame
        self._show(UtileriasFrame)
