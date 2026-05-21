import sys
import tkinter as tk
from tkinter import ttk, messagebox


def main():
    root = tk.Tk()
    root.withdraw()  # hide until login

    # Init DB
    try:
        from db.connection import DB
        DB.init()
        DB.conn()  # test connection
    except Exception as ex:
        messagebox.showerror("Error de conexión",
            f"No se pudo conectar a la base de datos.\n\n{ex}\n\n"
            "Verifica el archivo .env en pos-ui/")
        sys.exit(1)

    # Apply global ttk style
    from ui.theme import apply_ttk_style
    apply_ttk_style(ttk.Style(root))
    root.configure(bg="#0d1b2a")

    def on_login(usuario, turno):
        root.deiconify()
        root.title(f"POS ERP — {usuario['nombre']}")
        root.geometry("1280x800")
        root.minsize(900, 600)

        # Clear previous content
        for w in root.winfo_children():
            w.destroy()

        from ui.pos_window import POSWindow

        def on_logout():
            for w in root.winfo_children():
                w.destroy()
            root.withdraw()
            _show_login()

        POSWindow(root, usuario, turno, on_logout)

    def _show_login():
        from ui.login_window import LoginWindow
        LoginWindow(root, on_login)

    _show_login()
    root.mainloop()


if __name__ == "__main__":
    main()
