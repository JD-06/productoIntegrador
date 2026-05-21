import tkinter as tk
from tkinter import ttk, messagebox
from decimal import Decimal

import db.venta_dao as vdao
import db.cliente_dao as cdao
from services import cart_service as cart
from services import ticket_service as ticket
from ui import theme as T


class CobroDialog(tk.Toplevel):
    def __init__(self, master, usuario, turno, items, on_success):
        super().__init__(master)
        self.usuario   = usuario
        self.turno     = turno
        self.items     = items
        self.on_success = on_success

        totales = cart.calcular(items)
        self.subtotal = totales["subtotal"]
        self.iva      = totales["iva"]
        self.total    = totales["total"]

        self.metodos  = vdao.get_metodos_pago()
        self.clientes = cdao.find_all()

        self.title("Cobro")
        self.resizable(False, False)
        self.configure(bg=T.BG)
        self._build()
        self._center()
        self.grab_set()

    def _center(self):
        self.update_idletasks()
        w, h = 460, 560
        x = (self.winfo_screenwidth() - w) // 2
        y = (self.winfo_screenheight() - h) // 2
        self.geometry(f"{w}x{h}+{x}+{y}")

    def _build(self):
        p = tk.Frame(self, bg=T.BG, padx=30, pady=20)
        p.pack(fill="both", expand=True)

        tk.Label(p, text="Resumen de Venta", font=T.FONT_XLARGE,
                 bg=T.BG, fg=T.TEXT).pack(anchor="w", pady=(0, 16))

        # Totales
        card = tk.Frame(p, bg=T.CARD, pady=12, padx=16)
        card.pack(fill="x", pady=(0, 16))
        self._row(card, "Subtotal:", f"${self.subtotal:.2f}")
        self._row(card, "IVA (16%):", f"${self.iva:.2f}")
        self._row(card, "TOTAL:", f"${self.total:.2f}", bold=True, color=T.SUCCESS)

        # Método de pago
        tk.Label(p, text="Método de pago", bg=T.BG, fg=T.TEXT,
                 font=T.FONT_BOLD).pack(anchor="w")
        self.var_metodo = tk.StringVar()
        nombres = [m["nombre"] for m in self.metodos]
        cb = ttk.Combobox(p, textvariable=self.var_metodo,
                          values=nombres, state="readonly", font=T.FONT_NORMAL)
        cb.pack(fill="x", pady=(4, 12))
        if nombres:
            cb.current(0)

        # Cliente (opcional)
        tk.Label(p, text="Cliente (opcional)", bg=T.BG, fg=T.TEXT,
                 font=T.FONT_BOLD).pack(anchor="w")
        self.var_cliente = tk.StringVar(value="— Sin cliente —")
        opciones = ["— Sin cliente —"] + [c["nombre"] for c in self.clientes]
        ttk.Combobox(p, textvariable=self.var_cliente,
                     values=opciones, state="readonly",
                     font=T.FONT_NORMAL).pack(fill="x", pady=(4, 12))

        # Monto recibido
        tk.Label(p, text="Monto recibido ($)", bg=T.BG, fg=T.TEXT,
                 font=T.FONT_BOLD).pack(anchor="w")
        self.var_recibido = tk.StringVar(value=str(self.total))
        e = tk.Entry(p, textvariable=self.var_recibido,
                     bg=T.CARD, fg=T.TEXT, insertbackground=T.TEXT,
                     relief="flat", font=T.FONT_LARGE)
        e.pack(fill="x", ipady=8, pady=(4, 8))
        e.bind("<KeyRelease>", self._calc_cambio)

        self.lbl_cambio = tk.Label(p, text="Cambio: $0.00",
                                    bg=T.BG, fg=T.WARNING, font=T.FONT_LARGE)
        self.lbl_cambio.pack(anchor="w", pady=(0, 16))

        # Botones
        bf = tk.Frame(p, bg=T.BG)
        bf.pack(fill="x")
        tk.Button(bf, text="Cancelar", command=self.destroy,
                  bg=T.SURFACE, fg=T.TEXT, activebackground=T.CARD,
                  relief="flat", font=T.FONT_BOLD, cursor="hand2"
                  ).pack(side="left", ipady=8, ipadx=16)
        tk.Button(bf, text="COBRAR", command=self._cobrar,
                  bg=T.SUCCESS, fg=T.TEXT, activebackground="#2d9249",
                  relief="flat", font=T.FONT_BOLD, cursor="hand2"
                  ).pack(side="right", ipady=8, ipadx=24)

    def _row(self, parent, label, value, bold=False, color=None):
        f = tk.Frame(parent, bg=T.CARD)
        f.pack(fill="x", pady=2)
        font = T.FONT_BOLD if bold else T.FONT_NORMAL
        fg = color or T.TEXT
        tk.Label(f, text=label, bg=T.CARD, fg=T.MUTED, font=font,
                 width=16, anchor="w").pack(side="left")
        tk.Label(f, text=value, bg=T.CARD, fg=fg,
                 font=("Segoe UI", 13, "bold") if bold else font,
                 anchor="e").pack(side="right")

    def _calc_cambio(self, _=None):
        try:
            rec = Decimal(self.var_recibido.get())
            cambio = cart.calcular_cambio(self.total, rec)
            self.lbl_cambio.config(text=f"Cambio: ${cambio:.2f}")
        except Exception:
            self.lbl_cambio.config(text="Cambio: $0.00")

    def _cobrar(self):
        try:
            recibido = Decimal(self.var_recibido.get())
        except Exception:
            messagebox.showerror("Error", "Monto recibido inválido.", parent=self)
            return

        if recibido < self.total:
            messagebox.showerror("Error", "El monto recibido es menor al total.", parent=self)
            return

        metodo_nombre = self.var_metodo.get()
        metodo = next((m for m in self.metodos if m["nombre"] == metodo_nombre), None)
        if not metodo:
            messagebox.showerror("Error", "Selecciona un método de pago.", parent=self)
            return

        cambio = cart.calcular_cambio(self.total, recibido)

        cliente_id = None
        cn = self.var_cliente.get()
        if cn != "— Sin cliente —":
            cl = next((c for c in self.clientes if c["nombre"] == cn), None)
            if cl:
                cliente_id = cl["id"]

        db_items = [
            {
                "producto_id": i["producto_id"],
                "cantidad":    i["cantidad"],
                "precio":      i["precio"],
                "subtotal":    Decimal(str(i["precio"])) * i["cantidad"],
            }
            for i in self.items
        ]

        try:
            turno_id = self.turno["id"] if self.turno else None
            venta_id = vdao.insertar_venta(
                turno_id, self.usuario["id"], db_items,
                self.subtotal, self.iva, self.total,
                metodo["id"], recibido, cambio, cliente_id
            )
            # Puntos (1 punto por cada $10)
            if cliente_id:
                import db.cliente_dao as cdao2
                puntos = int(self.total / 10)
                if puntos > 0:
                    cdao2.agregar_puntos(cliente_id, puntos, venta_id)

            ticket_items = [
                {"nombre": i["nombre"], "cantidad": i["cantidad"],
                 "precio_unitario": i["precio"],
                 "subtotal": Decimal(str(i["precio"])) * i["cantidad"]}
                for i in self.items
            ]
            ticket.generar_ticket_txt(
                venta_id, self.usuario["nombre"], ticket_items,
                self.subtotal, self.iva, self.total,
                metodo_nombre, recibido, cambio
            )

            self.destroy()
            self.on_success(venta_id, cambio)
        except Exception as ex:
            messagebox.showerror("Error al cobrar", str(ex), parent=self)
