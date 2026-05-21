import tkinter as tk
from tkinter import ttk, messagebox
from decimal import Decimal

import db.usuario_dao as udao
import db.turno_dao as tdao
from ui import theme as T


class LoginWindow(tk.Toplevel):
    def __init__(self, master, on_success):
        super().__init__(master)
        self.on_success = on_success
        self.title("POS Empresarial — Iniciar sesión")
        self.resizable(False, False)
        self.configure(bg=T.BG)
        self._build()
        self._center()
        self.grab_set()

    def _center(self):
        self.update_idletasks()
        w, h = 420, 520
        x = (self.winfo_screenwidth() - w) // 2
        y = (self.winfo_screenheight() - h) // 2
        self.geometry(f"{w}x{h}+{x}+{y}")

    def _build(self):
        root = tk.Frame(self, bg=T.BG)
        root.pack(fill="both", expand=True, padx=40, pady=40)

        tk.Label(root, text="POS ERP", font=("Segoe UI", 28, "bold"),
                 bg=T.BG, fg=T.PRIMARY).pack(pady=(0, 4))
        tk.Label(root, text="Sistema de Punto de Venta",
                 font=T.FONT_NORMAL, bg=T.BG, fg=T.MUTED).pack(pady=(0, 30))

        # Usuario
        tk.Label(root, text="Usuario", bg=T.BG, fg=T.TEXT,
                 font=T.FONT_BOLD, anchor="w").pack(fill="x")
        self.var_user = tk.StringVar()
        e_user = tk.Entry(root, textvariable=self.var_user,
                          bg=T.CARD, fg=T.TEXT, insertbackground=T.TEXT,
                          relief="flat", font=T.FONT_NORMAL)
        e_user.pack(fill="x", ipady=8, pady=(4, 14))
        e_user.insert(0, "Admin")

        # PIN
        tk.Label(root, text="PIN", bg=T.BG, fg=T.TEXT,
                 font=T.FONT_BOLD, anchor="w").pack(fill="x")
        self.var_pin = tk.StringVar()
        e_pin = tk.Entry(root, textvariable=self.var_pin, show="●",
                         bg=T.CARD, fg=T.TEXT, insertbackground=T.TEXT,
                         relief="flat", font=T.FONT_NORMAL)
        e_pin.pack(fill="x", ipady=8, pady=(4, 14))
        e_pin.bind("<Return>", lambda e: self._login())

        # Fondo de caja (solo para cajeros)
        self._frame_fondo = tk.Frame(root, bg=T.BG)
        self._frame_fondo.pack(fill="x")
        tk.Label(self._frame_fondo, text="Fondo inicial ($)",
                 bg=T.BG, fg=T.TEXT, font=T.FONT_BOLD, anchor="w").pack(fill="x")
        self.var_fondo = tk.StringVar(value="0.00")
        tk.Entry(self._frame_fondo, textvariable=self.var_fondo,
                 bg=T.CARD, fg=T.TEXT, insertbackground=T.TEXT,
                 relief="flat", font=T.FONT_NORMAL).pack(fill="x", ipady=8, pady=(4, 14))

        btn = tk.Button(root, text="INGRESAR",
                        command=self._login,
                        bg=T.PRIMARY, fg=T.TEXT, activebackground="#1557b0",
                        activeforeground=T.TEXT, relief="flat",
                        font=T.FONT_BOLD, cursor="hand2")
        btn.pack(fill="x", ipady=10, pady=(10, 0))

        self.lbl_error = tk.Label(root, text="", bg=T.BG, fg=T.DANGER,
                                   font=T.FONT_NORMAL)
        self.lbl_error.pack(pady=10)

    def _login(self):
        nombre = self.var_user.get().strip()
        pin    = self.var_pin.get().strip()
        if not nombre or not pin:
            self.lbl_error.config(text="Ingresa usuario y PIN.")
            return

        user = udao.verificar_pin(nombre, pin)
        if not user:
            self.lbl_error.config(text="Usuario o PIN incorrecto.")
            return

        turno = None
        if user["rol"] != "ADMIN":
            try:
                fondo = Decimal(self.var_fondo.get() or "0")
            except Exception:
                fondo = Decimal("0")
            turno = tdao.abrir(user["id"], fondo)

        self.destroy()
        self.on_success(user, turno)
