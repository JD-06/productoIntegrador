import tkinter as tk
from tkinter import ttk, messagebox
from decimal import Decimal

import db.producto_dao as pdao
import db.categoria_dao as catdao
import db.turno_dao as tdao
from services import cart_service as cart
from ui.cobro_dialog import CobroDialog
from ui import theme as T


class POSWindow(tk.Frame):
    def __init__(self, master, usuario, turno, on_logout):
        super().__init__(master, bg=T.BG)
        self.usuario   = usuario
        self.turno     = turno
        self.on_logout = on_logout
        self.items     = []          # [{producto_id, nombre, precio, cantidad}]
        self.productos = []
        self.categorias = []
        self.pack(fill="both", expand=True)
        self._build()
        self._cargar_categorias()
        self._cargar_productos()
        self._bind_keys()

    # ── Build UI ────────────────────────────────────────────────────────────
    def _build(self):
        self._build_header()
        body = tk.Frame(self, bg=T.BG)
        body.pack(fill="both", expand=True)
        self._build_catalog(body)
        self._build_cart(body)

    def _build_header(self):
        h = tk.Frame(self, bg=T.HEADER_BG, pady=10, padx=16)
        h.pack(fill="x")
        tk.Label(h, text="POS ERP", font=T.FONT_LARGE,
                 bg=T.HEADER_BG, fg=T.PRIMARY).pack(side="left")
        info = f"  {self.usuario['nombre']}  |  Turno: {self.turno['codigo'] if self.turno else '—'}"
        tk.Label(h, text=info, font=T.FONT_NORMAL,
                 bg=T.HEADER_BG, fg=T.MUTED).pack(side="left", padx=12)

        btn_frame = tk.Frame(h, bg=T.HEADER_BG)
        btn_frame.pack(side="right")
        if self.usuario["rol"] == "ADMIN":
            tk.Button(btn_frame, text="Admin",
                      command=self._abrir_admin,
                      bg=T.WARNING, fg=T.BG, activebackground="#e6a800",
                      relief="flat", font=T.FONT_BOLD, cursor="hand2"
                      ).pack(side="left", padx=4, ipady=4, ipadx=10)
        tk.Button(btn_frame, text="Cerrar Turno",
                  command=self._cerrar_turno,
                  bg=T.DANGER, fg=T.TEXT, activebackground="#c0392b",
                  relief="flat", font=T.FONT_BOLD, cursor="hand2"
                  ).pack(side="left", padx=4, ipady=4, ipadx=10)

    def _build_catalog(self, parent):
        left = tk.Frame(parent, bg=T.BG)
        left.pack(side="left", fill="both", expand=True, padx=(12, 6), pady=12)

        # Search bar
        sf = tk.Frame(left, bg=T.BG)
        sf.pack(fill="x", pady=(0, 8))
        tk.Label(sf, text="🔍", bg=T.BG, fg=T.MUTED).pack(side="left")
        self.var_search = tk.StringVar()
        self.var_search.trace_add("write", lambda *_: self._filtrar())
        tk.Entry(sf, textvariable=self.var_search,
                 bg=T.CARD, fg=T.TEXT, insertbackground=T.TEXT,
                 relief="flat", font=T.FONT_NORMAL
                 ).pack(side="left", fill="x", expand=True, ipady=6, padx=6)

        # Category tabs
        self.cat_frame = tk.Frame(left, bg=T.BG)
        self.cat_frame.pack(fill="x", pady=(0, 8))

        # Product grid (canvas + scrollbar)
        canvas_frame = tk.Frame(left, bg=T.BG)
        canvas_frame.pack(fill="both", expand=True)
        self.canvas = tk.Canvas(canvas_frame, bg=T.BG, highlightthickness=0)
        sb = ttk.Scrollbar(canvas_frame, orient="vertical",
                            command=self.canvas.yview)
        self.canvas.configure(yscrollcommand=sb.set)
        sb.pack(side="right", fill="y")
        self.canvas.pack(side="left", fill="both", expand=True)
        self.grid_frame = tk.Frame(self.canvas, bg=T.BG)
        self._grid_window = self.canvas.create_window(
            (0, 0), window=self.grid_frame, anchor="nw"
        )
        self.grid_frame.bind("<Configure>", self._on_grid_configure)
        self.canvas.bind("<Configure>", self._on_canvas_configure)
        self.canvas.bind("<MouseWheel>", lambda e: self.canvas.yview_scroll(-1 * (e.delta // 120), "units"))
        self.canvas.bind("<Button-4>", lambda e: self.canvas.yview_scroll(-1, "units"))
        self.canvas.bind("<Button-5>", lambda e: self.canvas.yview_scroll(1, "units"))

    def _build_cart(self, parent):
        right = tk.Frame(parent, bg=T.CARD, width=320)
        right.pack(side="right", fill="y", padx=(6, 12), pady=12)
        right.pack_propagate(False)

        tk.Label(right, text="Carrito", font=T.FONT_LARGE,
                 bg=T.CARD, fg=T.TEXT).pack(anchor="w", padx=12, pady=10)

        ttk.Separator(right).pack(fill="x")

        # Cart list
        cols = ("Producto", "Cant", "Precio", "Subtotal")
        self.tree = ttk.Treeview(right, columns=cols, show="headings",
                                  height=12, selectmode="browse")
        for col, w in zip(cols, [130, 45, 70, 75]):
            self.tree.heading(col, text=col)
            self.tree.column(col, width=w, anchor="center")
        self.tree.pack(fill="x", padx=8, pady=8)

        # Totals
        tots = tk.Frame(right, bg=T.CARD, padx=12)
        tots.pack(fill="x")
        self.lbl_subtotal = tk.Label(tots, text="Subtotal: $0.00",
                                      bg=T.CARD, fg=T.MUTED, font=T.FONT_NORMAL)
        self.lbl_subtotal.pack(anchor="e")
        self.lbl_iva = tk.Label(tots, text="IVA: $0.00",
                                 bg=T.CARD, fg=T.MUTED, font=T.FONT_NORMAL)
        self.lbl_iva.pack(anchor="e")
        self.lbl_total = tk.Label(tots, text="TOTAL: $0.00",
                                   bg=T.CARD, fg=T.SUCCESS,
                                   font=("Segoe UI", 16, "bold"))
        self.lbl_total.pack(anchor="e", pady=4)

        ttk.Separator(right).pack(fill="x", pady=8)

        # Action buttons
        bf = tk.Frame(right, bg=T.CARD, padx=12)
        bf.pack(fill="x")
        tk.Button(bf, text="Eliminar (F11)",
                  command=self._eliminar_item,
                  bg=T.DANGER, fg=T.TEXT, activebackground="#c0392b",
                  relief="flat", font=T.FONT_BOLD, cursor="hand2"
                  ).pack(fill="x", ipady=6, pady=2)
        tk.Button(bf, text="Limpiar carrito",
                  command=self._limpiar,
                  bg=T.SURFACE, fg=T.TEXT, activebackground=T.BORDER,
                  relief="flat", font=T.FONT_BOLD, cursor="hand2"
                  ).pack(fill="x", ipady=6, pady=2)
        tk.Button(bf, text="COBRAR (F12)",
                  command=self._abrir_cobro,
                  bg=T.SUCCESS, fg=T.TEXT, activebackground="#2d9249",
                  relief="flat", font=("Segoe UI", 12, "bold"), cursor="hand2"
                  ).pack(fill="x", ipady=10, pady=(8, 2))

    # ── Data loading ─────────────────────────────────────────────────────────
    def _cargar_categorias(self):
        self.categorias = [{"id": None, "nombre": "Todos"}] + catdao.find_all()
        for w in self.cat_frame.winfo_children():
            w.destroy()
        self.var_cat = tk.IntVar(value=-1)
        for cat in self.categorias:
            cid = cat["id"] if cat["id"] is not None else -1
            rb = tk.Radiobutton(
                self.cat_frame, text=cat["nombre"],
                variable=self.var_cat, value=cid,
                command=self._filtrar,
                bg=T.BG, fg=T.TEXT, selectcolor=T.PRIMARY,
                activebackground=T.BG, activeforeground=T.TEXT,
                font=T.FONT_NORMAL, relief="flat", cursor="hand2",
                indicatoron=False, padx=10, pady=4
            )
            rb.pack(side="left", padx=2)
        self.var_cat.set(-1)

    def _cargar_productos(self):
        self.productos = pdao.find_all()
        self._filtrar()

    def _filtrar(self, *_):
        texto = self.var_search.get().strip()
        cat_id = self.var_cat.get() if hasattr(self, "var_cat") else -1

        if texto:
            prods = pdao.search(texto)
        elif cat_id == -1:
            prods = self.productos
        else:
            prods = [p for p in self.productos if p.get("categoria_id") == cat_id
                     or (cat_id != -1 and str(p.get("categoria", "")) ==
                         next((c["nombre"] for c in self.categorias if c["id"] == cat_id), ""))]
        self._render_grid(prods)

    def _render_grid(self, prods):
        for w in self.grid_frame.winfo_children():
            w.destroy()
        COLS = 4
        for i, p in enumerate(prods):
            r, c = divmod(i, COLS)
            card = tk.Frame(self.grid_frame, bg=T.CARD,
                            padx=8, pady=8, cursor="hand2")
            card.grid(row=r, column=c, padx=4, pady=4, sticky="nsew")
            self.grid_frame.columnconfigure(c, weight=1)

            tk.Label(card, text=p["nombre"][:24],
                     bg=T.CARD, fg=T.TEXT, font=T.FONT_BOLD,
                     wraplength=120, justify="center").pack()
            tk.Label(card, text=f"${Decimal(str(p['precio'])):.2f}",
                     bg=T.CARD, fg=T.SUCCESS,
                     font=("Segoe UI", 12, "bold")).pack()
            stock_color = T.DANGER if p.get("stock", 0) <= 0 else T.MUTED
            tk.Label(card, text=f"Stock: {p.get('stock', 0)}",
                     bg=T.CARD, fg=stock_color, font=T.FONT_NORMAL).pack()

            card.bind("<Button-1>", lambda e, prod=p: self._agregar(prod))
            for child in card.winfo_children():
                child.bind("<Button-1>", lambda e, prod=p: self._agregar(prod))

    # ── Cart actions ─────────────────────────────────────────────────────────
    def _agregar(self, prod):
        existing = next((i for i in self.items if i["producto_id"] == prod["id"]), None)
        if existing:
            existing["cantidad"] += 1
        else:
            self.items.append({
                "producto_id": prod["id"],
                "nombre":      prod["nombre"],
                "precio":      Decimal(str(prod["precio"])),
                "cantidad":    1,
            })
        self._refresh_cart()

    def _eliminar_item(self):
        sel = self.tree.selection()
        if not sel:
            return
        idx = self.tree.index(sel[0])
        if 0 <= idx < len(self.items):
            self.items.pop(idx)
            self._refresh_cart()

    def _limpiar(self):
        self.items.clear()
        self._refresh_cart()

    def _refresh_cart(self):
        for row in self.tree.get_children():
            self.tree.delete(row)
        for item in self.items:
            sub = item["precio"] * item["cantidad"]
            self.tree.insert("", "end", values=(
                item["nombre"][:20],
                item["cantidad"],
                f"${item['precio']:.2f}",
                f"${sub:.2f}",
            ))
        if self.items:
            tots = cart.calcular(self.items)
            self.lbl_subtotal.config(text=f"Subtotal: ${tots['subtotal']:.2f}")
            self.lbl_iva.config(text=f"IVA: ${tots['iva']:.2f}")
            self.lbl_total.config(text=f"TOTAL: ${tots['total']:.2f}")
        else:
            self.lbl_subtotal.config(text="Subtotal: $0.00")
            self.lbl_iva.config(text="IVA: $0.00")
            self.lbl_total.config(text="TOTAL: $0.00")

    def _abrir_cobro(self):
        if not self.items:
            messagebox.showwarning("Carrito vacío", "Agrega productos al carrito.", parent=self)
            return
        CobroDialog(self, self.usuario, self.turno, self.items, self._on_venta_ok)

    def _on_venta_ok(self, venta_id, cambio):
        messagebox.showinfo("Venta completada",
                            f"Folio #{venta_id:06d}\nCambio: ${cambio:.2f}",
                            parent=self)
        self._limpiar()
        self._cargar_productos()

    def _cerrar_turno(self):
        if not messagebox.askyesno("Cerrar turno",
                                    "¿Cerrar turno y salir?", parent=self):
            return
        if self.turno:
            tdao.cerrar(self.turno["id"])
        self.on_logout()

    def _abrir_admin(self):
        from ui.admin_window import AdminWindow
        AdminWindow(self.winfo_toplevel(), self.usuario)

    # ── Canvas resize ────────────────────────────────────────────────────────
    def _on_grid_configure(self, _):
        self.canvas.configure(scrollregion=self.canvas.bbox("all"))

    def _on_canvas_configure(self, event):
        self.canvas.itemconfig(self._grid_window, width=event.width)

    # ── Key bindings ─────────────────────────────────────────────────────────
    def _bind_keys(self):
        root = self.winfo_toplevel()
        root.bind("<F12>", lambda e: self._abrir_cobro())
        root.bind("<F11>", lambda e: self._eliminar_item())
        root.bind("<F5>",  lambda e: self.var_search.get() and
                  self.var_search.set("") or None)
