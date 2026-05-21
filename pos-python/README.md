# POS Python

Versión Python del sistema POS Empresarial ERP. Implementada con Tkinter y psycopg2, conectada a la misma base de datos PostgreSQL que la versión Java.

## Requisitos

- Python 3.10+
- PostgreSQL accesible (configurado en `.env`)
- `tk` instalado en el sistema (`sudo pacman -S tk` en Arch/CachyOS)

## Instalación

```bash
pip install -r requirements.txt
```

## Ejecución

```bash
python main.py
```

La app busca el archivo `.env` en `../puntoDeVenta/pos-ui/.env` automáticamente.

## Credenciales por defecto

| Usuario | PIN  | Rol   |
|---------|------|-------|
| Admin   | 1234 | ADMIN |
| Cajero  | 1234 | CAJERO |

## Atajos de teclado (pantalla POS)

| Tecla | Acción |
|-------|--------|
| `F12` | Abrir cobro |
| `F11` | Eliminar ítem seleccionado |
| `F5`  | Limpiar búsqueda |

## Dependencias

```
psycopg2-binary   # Conexión PostgreSQL
python-dotenv     # Carga de variables de entorno desde .env
Pillow            # Manejo de imágenes de productos
```
