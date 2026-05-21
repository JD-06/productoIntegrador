# POS Empresarial ERP

Sistema de Punto de Venta empresarial con módulos ERP integrados. Implementado en dos versiones: **Java (JavaFX)** y **Python (Tkinter)**, ambas conectadas a la misma base de datos PostgreSQL.

---

## Estructura del repositorio

```
productoIntegrador/
├── puntoDeVenta/          # Aplicación Java (JavaFX + Maven)
│   ├── pos-core/          # Entidades y servicios de negocio
│   ├── pos-data/          # Capa de acceso a datos (DAOs + Flyway)
│   ├── pos-ui/            # Interfaz gráfica JavaFX + controladores
│   ├── pos-sync/          # Worker de sincronización en background
│   ├── pos-fiscal/        # Módulo fiscal (CFDI)
│   ├── pos-peripherals/   # Periféricos (impresoras, cajón)
│   ├── pos-reports/       # Generación de reportes (JasperReports)
│   └── pos-admin/         # Módulo administrativo extendido
├── pos-python/            # Aplicación Python (Tkinter)
│   ├── db/                # DAOs en Python (psycopg2)
│   ├── services/          # Lógica de negocio
│   ├── ui/                # Interfaz gráfica Tkinter
│   └── main.py            # Punto de entrada
├── productos_imagenes/    # Imágenes de productos
├── productos.json         # Catálogo de productos (importación)
└── supermarket_scraper.py # Script de scraping de productos
```

---

## Requisitos generales

- **PostgreSQL 14+** (base de datos compartida entre ambas versiones)
- **Java 21** (para la versión Java)
- **Maven 3.8+** (para la versión Java)
- **Python 3.10+** (para la versión Python)

---

## Configuración

Ambas versiones leen sus credenciales desde un archivo `.env`. Crea el archivo en `puntoDeVenta/pos-ui/.env`:

```env
DB_URL=jdbc:postgresql://HOST:5432/DATABASE?sslmode=disable
DB_USER=tu_usuario
DB_PASSWORD=tu_contraseña
IMAGES_PATH=/ruta/a/productos_imagenes
JSON_PATH=/ruta/a/productos.json
```

> **Nota:** El archivo `.env` está en `.gitignore` y **nunca** debe subirse al repositorio.

---

## Versión Java

### Tecnologías

| Componente | Tecnología |
|-----------|-----------|
| Interfaz  | JavaFX 21 + FXML |
| Base de datos | PostgreSQL + JDBC |
| Migraciones | Flyway 10 |
| ORM | jOOQ 3.19 |
| Inyección de dependencias | Google Guice |
| Reportes | JasperReports 7 |
| Serialización JSON | Jackson |
| HTTP | OkHttp |
| Logging | SLF4J + Logback |

### Instalación y ejecución

```bash
cd puntoDeVenta

# Compilar e instalar todos los módulos
mvn clean install -DskipTests

# Ejecutar la aplicación
cd pos-ui
mvn javafx:run
```

### Arquitectura

```
pos-parent (POM raíz)
├── pos-core      → Entidades (Producto, Venta) + CartService
├── pos-data      → DAOs, DatabaseManager, AppContext singleton
├── pos-ui        → Controladores JavaFX, ventanas FXML
├── pos-sync      → SyncWorker (thread daemon de sincronización)
├── pos-fiscal    → Integración fiscal / CFDI
├── pos-peripherals → Cajón de dinero, impresoras térmicas
├── pos-reports   → Exportación PDF/Excel con JasperReports
└── pos-admin     → Dashboard administrativo extendido
```

### Credenciales por defecto

| Usuario | PIN  | Rol    |
|---------|------|--------|
| Admin   | 1234 | ADMIN  |
| Cajero  | 1234 | CAJERO |

---

## Versión Python

Implementación equivalente usando Tkinter y psycopg2, sin dependencias externas de GUI.

### Tecnologías

| Componente | Tecnología |
|-----------|-----------|
| Interfaz | Tkinter + ttk |
| Base de datos | psycopg2 |
| Configuración | python-dotenv |
| Imágenes | Pillow |

### Instalación y ejecución

```bash
cd pos-python

# Instalar dependencias
pip install -r requirements.txt

# Ejecutar
python main.py
```

### Estructura interna

```
pos-python/
├── main.py                  # Arranque: carga .env, conecta DB, lanza login
├── requirements.txt
├── db/
│   ├── connection.py        # Singleton DB con psycopg2
│   ├── producto_dao.py      # CRUD productos
│   ├── venta_dao.py         # Ventas + detalle (transacción atómica)
│   ├── usuario_dao.py       # Usuarios + verificación PIN SHA-256
│   ├── turno_dao.py         # Turnos de caja
│   ├── categoria_dao.py     # Categorías
│   ├── inventario_dao.py    # Stock + movimientos
│   ├── cliente_dao.py       # CRM clientes + puntos de lealtad
│   ├── compra_dao.py        # Compras a proveedores
│   ├── nomina_dao.py        # Nómina por período
│   ├── cuenta_dao.py        # Cuentas por cobrar / pagar
│   ├── empleado_dao.py      # Empleados
│   └── log_dao.py           # Log de auditoría
├── services/
│   ├── cart_service.py      # Cálculo de subtotal, IVA (16%), total, cambio
│   └── ticket_service.py    # Generación de ticket TXT + exportación CSV
└── ui/
    ├── theme.py             # Paleta de colores y estilos ttk
    ├── login_window.py      # Pantalla de login con PIN
    ├── pos_window.py        # Pantalla principal del POS
    ├── cobro_dialog.py      # Diálogo de cobro (efectivo/tarjeta)
    ├── admin_window.py      # Panel admin con sidebar de módulos
    └── admin/
        ├── catalogo_frame.py    # CRUD catálogo de productos
        ├── categorias_frame.py  # CRUD categorías
        ├── inventario_frame.py  # Gestión de stock (entradas/salidas)
        ├── compras_frame.py     # Registro de compras a proveedores
        ├── crm_frame.py         # Directorio de clientes
        ├── cxc_frame.py         # Cuentas por cobrar
        ├── cxp_frame.py         # Cuentas por pagar
        ├── nomina_frame.py      # Nómina por período
        ├── empleados_frame.py   # Gestión de empleados
        ├── usuarios_frame.py    # Gestión de usuarios y PINs
        ├── monitor_frame.py     # Monitor de turnos y ventas en tiempo real
        ├── log_frame.py         # Log de auditoría
        └── utilerias_frame.py   # Exportación CSV + info del sistema
```

---

## Esquema de base de datos

La base de datos se inicializa automáticamente con Flyway (versión Java) en el primer arranque.

### Tablas principales

| Tabla | Descripción |
|-------|------------|
| `roles` / `usuarios` | Autenticación con PIN hasheado SHA-256 |
| `categorias` / `productos` | Catálogo de productos |
| `inventario` / `movimientos_inventario` | Control de stock |
| `turnos` | Turnos de caja con fondo inicial |
| `ventas` / `detalle_ventas` | Registro de ventas con IVA |
| `clientes` / `transacciones_puntos` | CRM y programa de lealtad |
| `proveedores` / `compras` | Compras a proveedores |
| `cuentas_cobrar` / `cuentas_pagar` | Cartera CxC/CxP |
| `empleados` / `nomina` | Nómina por período |
| `log_auditoria` | Trazabilidad de acciones |
| `permisos` / `roles_permisos` | Control de acceso por rol |

### Migraciones Flyway

| Versión | Descripción |
|---------|------------|
| V1 | Esquema completo (todas las tablas + índices) |
| V2 | Campos adicionales en productos (marca, unidad, imagen_local) |
| V3 | Seed de roles y usuario Admin (PIN: 1234) |
| V4 | Seed de usuario Cajero por defecto (PIN: 1234) |

---

## Funcionalidades por módulo

### POS (Punto de Venta)
- Catálogo de productos con búsqueda y filtro por categoría
- Carrito de compras con cálculo automático de IVA (16%)
- Cobro en efectivo o tarjeta con cálculo de cambio
- Generación de ticket en formato TXT
- Atajos de teclado: `F12` cobrar, `F11` eliminar ítem, `F5` limpiar búsqueda

### Administración
- **Catálogo:** alta, baja y modificación de productos con SKU y categoría
- **Inventario:** entradas, salidas manuales y alertas de stock mínimo
- **Compras:** registro de compras a proveedores con actualización automática de stock
- **CRM:** directorio de clientes con programa de puntos (1 pto / $10)
- **CxC/CxP:** cuentas por cobrar y por pagar con registro de pagos parciales
- **Nómina:** períodos de nómina con cálculo de deducciones y neto a pagar
- **Empleados:** gestión del personal vinculado a usuarios del sistema
- **Usuarios:** alta de usuarios, cambio de PIN, activar/desactivar
- **Monitor:** KPIs en tiempo real (ventas, monto total, turnos activos)
- **Log de auditoría:** historial de acciones del sistema
- **Utilerías:** exportación a CSV de productos, clientes e inventario

---

## Seguridad

- Los PINs se almacenan como hash SHA-256 (nunca en texto plano)
- El archivo `.env` con credenciales está excluido del repositorio vía `.gitignore`
- Los roles `ADMIN`, `CAJERO` y `SUPERVISOR` controlan el acceso a módulos
- El log de auditoría registra todas las operaciones críticas

---

## Licencia

Proyecto académico — Producto Integrador.
