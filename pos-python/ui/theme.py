BG       = "#0d1b2a"
SURFACE  = "#1b2838"
CARD     = "#243447"
BORDER   = "#2d4263"
PRIMARY  = "#1a73e8"
SUCCESS  = "#34a853"
DANGER   = "#ea4335"
WARNING  = "#fbbc04"
TEXT     = "#e8eaed"
MUTED    = "#9aa0a6"
HEADER_BG = "#0a1628"

FONT_NORMAL  = ("Segoe UI", 10)
FONT_BOLD    = ("Segoe UI", 10, "bold")
FONT_LARGE   = ("Segoe UI", 13, "bold")
FONT_XLARGE  = ("Segoe UI", 18, "bold")
FONT_MONO    = ("Courier New", 10)

BTN_PAD = {"padx": 12, "pady": 6}


def apply_ttk_style(style):
    """Apply dark theme to ttk widgets."""
    style.theme_use("clam")

    style.configure(".",
        background=BG, foreground=TEXT,
        font=FONT_NORMAL, borderwidth=0, relief="flat"
    )
    style.configure("TFrame", background=BG)
    style.configure("TLabel", background=BG, foreground=TEXT, font=FONT_NORMAL)
    style.configure("TButton",
        background=PRIMARY, foreground=TEXT, font=FONT_BOLD,
        padding=(10, 6), relief="flat", borderwidth=0
    )
    style.map("TButton",
        background=[("active", "#1557b0"), ("disabled", BORDER)],
        foreground=[("disabled", MUTED)]
    )
    style.configure("Danger.TButton", background=DANGER)
    style.map("Danger.TButton", background=[("active", "#c0392b")])
    style.configure("Success.TButton", background=SUCCESS)
    style.map("Success.TButton", background=[("active", "#2d9249")])
    style.configure("Warning.TButton", background=WARNING, foreground=BG)
    style.map("Warning.TButton", background=[("active", "#e6a800")])

    style.configure("TEntry",
        fieldbackground=CARD, foreground=TEXT,
        insertcolor=TEXT, bordercolor=BORDER,
        relief="flat", padding=6
    )
    style.configure("TCombobox",
        fieldbackground=CARD, foreground=TEXT,
        background=CARD, selectbackground=PRIMARY
    )
    style.configure("Treeview",
        background=CARD, foreground=TEXT,
        fieldbackground=CARD, rowheight=28,
        borderwidth=0
    )
    style.configure("Treeview.Heading",
        background=SURFACE, foreground=TEXT,
        font=FONT_BOLD, relief="flat", borderwidth=0
    )
    style.map("Treeview",
        background=[("selected", PRIMARY)],
        foreground=[("selected", TEXT)]
    )
    style.configure("TNotebook", background=BG, borderwidth=0)
    style.configure("TNotebook.Tab",
        background=SURFACE, foreground=TEXT,
        padding=(14, 8), font=FONT_BOLD
    )
    style.map("TNotebook.Tab",
        background=[("selected", PRIMARY)],
        foreground=[("selected", TEXT)]
    )
    style.configure("TScrollbar",
        background=SURFACE, troughcolor=BG,
        arrowcolor=MUTED, borderwidth=0
    )
    style.configure("TSeparator", background=BORDER)
    style.configure("Card.TFrame", background=CARD, relief="flat")
    style.configure("Header.TFrame", background=HEADER_BG)
    style.configure("Header.TLabel", background=HEADER_BG, foreground=TEXT)
    style.configure("Muted.TLabel", foreground=MUTED)
    style.configure("Title.TLabel", font=FONT_XLARGE, foreground=TEXT, background=BG)
    style.configure("Subtitle.TLabel", font=FONT_LARGE, foreground=MUTED, background=BG)
