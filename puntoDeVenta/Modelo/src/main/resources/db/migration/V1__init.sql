-- ============================================================
--  POS Empresarial ERP — Migración inicial (PostgreSQL)
--  V1 — Estructura completa: Fases 1 a 5
-- ============================================================

-- 4.1  USUARIOS Y SEGURIDAD
CREATE TABLE IF NOT EXISTS roles (
  id     SERIAL PRIMARY KEY,
  nombre VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS usuarios (
  id        SERIAL PRIMARY KEY,
  nombre    VARCHAR(100) NOT NULL,
  pin_hash  VARCHAR(255) NOT NULL,
  rol_id    INT NOT NULL REFERENCES roles(id),
  activo    BOOLEAN DEFAULT TRUE,
  creado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS log_auditoria (
  id             SERIAL PRIMARY KEY,
  usuario_id     INT REFERENCES usuarios(id),
  accion         VARCHAR(100) NOT NULL,
  tabla_afectada VARCHAR(50),
  registro_id    INT,
  detalle        JSONB,
  creado_en      TIMESTAMP DEFAULT NOW()
);

-- 4.2  CATALOGO
CREATE TABLE IF NOT EXISTS categorias (
  id      SERIAL PRIMARY KEY,
  nombre  VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS productos (
  id           SERIAL PRIMARY KEY,
  sku          VARCHAR(20)   NOT NULL UNIQUE,
  nombre       VARCHAR(150)  NOT NULL,
  categoria_id INT REFERENCES categorias(id),
  precio       NUMERIC(10,2) NOT NULL,
  activo       BOOLEAN DEFAULT TRUE,
  creado_en    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS inventario (
  producto_id  INT PRIMARY KEY REFERENCES productos(id),
  stock_actual INT NOT NULL DEFAULT 0,
  stock_minimo INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS movimientos_inventario (
  id            SERIAL PRIMARY KEY,
  producto_id   INT NOT NULL REFERENCES productos(id),
  tipo          VARCHAR(20) NOT NULL,
  cantidad      INT NOT NULL,
  referencia_id INT,
  usuario_id    INT REFERENCES usuarios(id),
  creado_en     TIMESTAMP DEFAULT NOW()
);

-- 4.3  TURNOS Y VENTAS
CREATE TABLE IF NOT EXISTS metodos_pago (
  id      SERIAL PRIMARY KEY,
  nombre  VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS clientes (
  id                SERIAL PRIMARY KEY,
  codigo            VARCHAR(20)  NOT NULL UNIQUE,
  nombre            VARCHAR(150) NOT NULL,
  rfc               VARCHAR(15),
  tipo              VARCHAR(20)  NOT NULL,
  puntos_acumulados INT DEFAULT 0,
  creado_en         TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS turnos (
  id                 SERIAL PRIMARY KEY,
  codigo             VARCHAR(20)   NOT NULL UNIQUE,
  cajero_id          INT NOT NULL REFERENCES usuarios(id),
  fondo_inicial      NUMERIC(10,2) NOT NULL DEFAULT 0,
  efectivo_ingresado NUMERIC(10,2) DEFAULT 0,
  cobro_tarjeta      NUMERIC(10,2) DEFAULT 0,
  venta_total        NUMERIC(10,2) DEFAULT 0,
  estado             VARCHAR(15) DEFAULT 'ABIERTO',
  abierto_en         TIMESTAMP DEFAULT NOW(),
  cerrado_en         TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ventas (
  id             SERIAL PRIMARY KEY,
  turno_id       INT NOT NULL REFERENCES turnos(id),
  cajero_id      INT NOT NULL REFERENCES usuarios(id),
  cliente_id     INT REFERENCES clientes(id),
  subtotal       NUMERIC(10,2) NOT NULL,
  iva            NUMERIC(10,2) NOT NULL,
  total          NUMERIC(10,2) NOT NULL,
  metodo_pago_id INT NOT NULL REFERENCES metodos_pago(id),
  monto_recibido NUMERIC(10,2),
  cambio         NUMERIC(10,2),
  estado         VARCHAR(15) DEFAULT 'COMPLETADA',
  creado_en      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS detalle_ventas (
  id              SERIAL PRIMARY KEY,
  venta_id        INT NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
  producto_id     INT NOT NULL REFERENCES productos(id),
  cantidad        INT NOT NULL,
  precio_unitario NUMERIC(10,2) NOT NULL,
  descuento       NUMERIC(10,2) DEFAULT 0,
  subtotal        NUMERIC(10,2) NOT NULL
);

-- 4.4  CRM Y LEALTAD
CREATE TABLE IF NOT EXISTS transacciones_puntos (
  id         SERIAL PRIMARY KEY,
  cliente_id INT NOT NULL REFERENCES clientes(id),
  venta_id   INT REFERENCES ventas(id),
  puntos     INT NOT NULL,
  motivo     VARCHAR(100),
  creado_en  TIMESTAMP DEFAULT NOW()
);

-- 4.5  ERP
CREATE TABLE IF NOT EXISTS proveedores (
  id       SERIAL PRIMARY KEY,
  nombre   VARCHAR(150) NOT NULL,
  rfc      VARCHAR(15),
  contacto VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS compras (
  id           SERIAL PRIMARY KEY,
  proveedor_id INT REFERENCES proveedores(id),
  total        NUMERIC(10,2) NOT NULL,
  usuario_id   INT REFERENCES usuarios(id),
  creado_en    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS detalle_compras (
  id             SERIAL PRIMARY KEY,
  compra_id      INT NOT NULL REFERENCES compras(id) ON DELETE CASCADE,
  producto_id    INT NOT NULL REFERENCES productos(id),
  cantidad       INT NOT NULL,
  costo_unitario NUMERIC(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS cuentas_cobrar (
  id          SERIAL PRIMARY KEY,
  cliente_id  INT NOT NULL REFERENCES clientes(id),
  venta_id    INT REFERENCES ventas(id),
  monto       NUMERIC(10,2) NOT NULL,
  saldo       NUMERIC(10,2) NOT NULL,
  estado      VARCHAR(15) DEFAULT 'PENDIENTE',
  vencimiento DATE,
  creado_en   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cuentas_pagar (
  id           SERIAL PRIMARY KEY,
  proveedor_id INT NOT NULL REFERENCES proveedores(id),
  compra_id    INT REFERENCES compras(id),
  monto        NUMERIC(10,2) NOT NULL,
  saldo        NUMERIC(10,2) NOT NULL,
  estado       VARCHAR(15) DEFAULT 'PENDIENTE',
  vencimiento  DATE,
  creado_en    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS empleados (
  id         SERIAL PRIMARY KEY,
  usuario_id INT UNIQUE REFERENCES usuarios(id),
  puesto     VARCHAR(80),
  salario    NUMERIC(10,2) NOT NULL,
  activo     BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS periodos_nomina (
  id        SERIAL PRIMARY KEY,
  periodo   VARCHAR(30) NOT NULL,
  creado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS nomina (
  id           SERIAL PRIMARY KEY,
  periodo_id   INT NOT NULL REFERENCES periodos_nomina(id),
  empleado_id  INT NOT NULL REFERENCES empleados(id),
  salario_base NUMERIC(10,2) NOT NULL,
  deducciones  NUMERIC(10,2) DEFAULT 0,
  neto_pagar   NUMERIC(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS permisos (
  id          SERIAL PRIMARY KEY,
  clave       VARCHAR(60) NOT NULL UNIQUE,
  descripcion VARCHAR(120)
);

CREATE TABLE IF NOT EXISTS roles_permisos (
  rol_id     INT NOT NULL REFERENCES roles(id),
  permiso_id INT NOT NULL REFERENCES permisos(id),
  PRIMARY KEY (rol_id, permiso_id)
);

-- INDICES
CREATE INDEX IF NOT EXISTS idx_ventas_turno  ON ventas(turno_id);
CREATE INDEX IF NOT EXISTS idx_ventas_cajero ON ventas(cajero_id);
CREATE INDEX IF NOT EXISTS idx_ventas_creado ON ventas(creado_en);
CREATE INDEX IF NOT EXISTS idx_detalle_venta ON detalle_ventas(venta_id);
CREATE INDEX IF NOT EXISTS idx_inv_stock     ON inventario(stock_actual);
CREATE INDEX IF NOT EXISTS idx_log_creado    ON log_auditoria(creado_en);
CREATE INDEX IF NOT EXISTS idx_mov_producto  ON movimientos_inventario(producto_id);
CREATE INDEX IF NOT EXISTS idx_cxc_estado    ON cuentas_cobrar(estado);
CREATE INDEX IF NOT EXISTS idx_cxp_estado    ON cuentas_pagar(estado);
