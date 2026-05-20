-- ============================================================
--  POS EMPRESARIAL ERP / CRM  —  Schema PostgreSQL
--  Versión 1.0  |  Fases 1 – 5
--  Base de datos: pos_erp
-- ============================================================

-- ──────────────────────────────────────────────────────────────
-- 4.1  USUARIOS Y SEGURIDAD
-- ──────────────────────────────────────────────────────────────

CREATE TABLE roles (
  id     SERIAL PRIMARY KEY,
  nombre VARCHAR(30) NOT NULL UNIQUE   -- 'ADMIN', 'CAJERO', 'SUPERVISOR'
);

CREATE TABLE usuarios (
  id        SERIAL PRIMARY KEY,
  nombre    VARCHAR(100) NOT NULL,
  pin_hash  VARCHAR(255) NOT NULL,     -- bcrypt cost-10
  rol_id    INT NOT NULL REFERENCES roles(id),
  activo    BOOLEAN DEFAULT TRUE,
  creado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE log_auditoria (
  id              SERIAL PRIMARY KEY,
  usuario_id      INT REFERENCES usuarios(id),
  accion          VARCHAR(100) NOT NULL,   -- 'VENTA_CREADA', 'PRODUCTO_EDITADO', …
  tabla_afectada  VARCHAR(50),
  registro_id     INT,
  detalle         JSONB,
  creado_en       TIMESTAMP DEFAULT NOW()
);

-- ──────────────────────────────────────────────────────────────
-- 4.2  CATÁLOGO
-- ──────────────────────────────────────────────────────────────

CREATE TABLE categorias (
  id      SERIAL PRIMARY KEY,
  nombre  VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE productos (
  id           SERIAL PRIMARY KEY,
  sku          VARCHAR(20)  NOT NULL UNIQUE,
  nombre       VARCHAR(150) NOT NULL,
  categoria_id INT REFERENCES categorias(id),
  precio       NUMERIC(10,2) NOT NULL,
  activo       BOOLEAN DEFAULT TRUE,
  creado_en    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE inventario (
  producto_id  INT PRIMARY KEY REFERENCES productos(id),
  stock_actual INT NOT NULL DEFAULT 0,
  stock_minimo INT NOT NULL DEFAULT 0
);

CREATE TABLE movimientos_inventario (
  id            SERIAL PRIMARY KEY,
  producto_id   INT NOT NULL REFERENCES productos(id),
  tipo          VARCHAR(20) NOT NULL,   -- 'ENTRADA', 'SALIDA', 'AJUSTE'
  cantidad      INT NOT NULL,
  referencia_id INT,                    -- venta_id o compra_id
  usuario_id    INT REFERENCES usuarios(id),
  creado_en     TIMESTAMP DEFAULT NOW()
);

-- ──────────────────────────────────────────────────────────────
-- 4.3  TURNOS Y VENTAS  (POS)
-- ──────────────────────────────────────────────────────────────

CREATE TABLE metodos_pago (
  id      SERIAL PRIMARY KEY,
  nombre  VARCHAR(30) NOT NULL UNIQUE   -- 'EFECTIVO', 'TARJETA'
);

CREATE TABLE clientes (
  id                SERIAL PRIMARY KEY,
  codigo            VARCHAR(20) NOT NULL UNIQUE,   -- 'C-001'
  nombre            VARCHAR(150) NOT NULL,
  rfc               VARCHAR(15),
  tipo              VARCHAR(20) NOT NULL,           -- 'MAYORISTA', 'MENUDEO'
  puntos_acumulados INT DEFAULT 0,
  creado_en         TIMESTAMP DEFAULT NOW()
);

CREATE TABLE turnos (
  id                 SERIAL PRIMARY KEY,
  codigo             VARCHAR(20) NOT NULL UNIQUE,   -- 'CX-0901'
  cajero_id          INT NOT NULL REFERENCES usuarios(id),
  fondo_inicial      NUMERIC(10,2) NOT NULL DEFAULT 0,
  efectivo_ingresado NUMERIC(10,2) DEFAULT 0,
  cobro_tarjeta      NUMERIC(10,2) DEFAULT 0,
  venta_total        NUMERIC(10,2) DEFAULT 0,
  estado             VARCHAR(15) DEFAULT 'ABIERTO', -- 'ABIERTO', 'CERRADA'
  abierto_en         TIMESTAMP DEFAULT NOW(),
  cerrado_en         TIMESTAMP
);

CREATE TABLE ventas (
  id             SERIAL PRIMARY KEY,
  turno_id       INT NOT NULL REFERENCES turnos(id),
  cajero_id      INT NOT NULL REFERENCES usuarios(id),
  cliente_id     INT REFERENCES clientes(id),       -- nullable
  subtotal       NUMERIC(10,2) NOT NULL,
  iva            NUMERIC(10,2) NOT NULL,
  total          NUMERIC(10,2) NOT NULL,
  metodo_pago_id INT NOT NULL REFERENCES metodos_pago(id),
  monto_recibido NUMERIC(10,2),
  cambio         NUMERIC(10,2),
  estado         VARCHAR(15) DEFAULT 'COMPLETADA',  -- 'COMPLETADA', 'CANCELADA', 'DEVOLUCION'
  creado_en      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE detalle_ventas (
  id              SERIAL PRIMARY KEY,
  venta_id        INT NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
  producto_id     INT NOT NULL REFERENCES productos(id),
  cantidad        INT NOT NULL,
  precio_unitario NUMERIC(10,2) NOT NULL,
  descuento       NUMERIC(10,2) DEFAULT 0,
  subtotal        NUMERIC(10,2) NOT NULL
);

-- ──────────────────────────────────────────────────────────────
-- 4.4  CRM Y LEALTAD
-- ──────────────────────────────────────────────────────────────

CREATE TABLE transacciones_puntos (
  id         SERIAL PRIMARY KEY,
  cliente_id INT NOT NULL REFERENCES clientes(id),
  venta_id   INT REFERENCES ventas(id),
  puntos     INT NOT NULL,               -- positivo = ganados, negativo = canjeados
  motivo     VARCHAR(100),
  creado_en  TIMESTAMP DEFAULT NOW()
);

-- ──────────────────────────────────────────────────────────────
-- 4.5  ERP  — Compras, CxC, CxP, Nómina
-- ──────────────────────────────────────────────────────────────

CREATE TABLE proveedores (
  id       SERIAL PRIMARY KEY,
  nombre   VARCHAR(150) NOT NULL,
  rfc      VARCHAR(15),
  contacto VARCHAR(100)
);

CREATE TABLE compras (
  id           SERIAL PRIMARY KEY,
  proveedor_id INT REFERENCES proveedores(id),
  total        NUMERIC(10,2) NOT NULL,
  usuario_id   INT REFERENCES usuarios(id),
  creado_en    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE detalle_compras (
  id             SERIAL PRIMARY KEY,
  compra_id      INT NOT NULL REFERENCES compras(id) ON DELETE CASCADE,
  producto_id    INT NOT NULL REFERENCES productos(id),
  cantidad       INT NOT NULL,
  costo_unitario NUMERIC(10,2) NOT NULL
);

CREATE TABLE cuentas_cobrar (
  id          SERIAL PRIMARY KEY,
  cliente_id  INT NOT NULL REFERENCES clientes(id),
  venta_id    INT REFERENCES ventas(id),
  monto       NUMERIC(10,2) NOT NULL,
  saldo       NUMERIC(10,2) NOT NULL,
  estado      VARCHAR(15) DEFAULT 'PENDIENTE',   -- 'PENDIENTE', 'PAGADA', 'VENCIDA'
  vencimiento DATE,
  creado_en   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE cuentas_pagar (
  id           SERIAL PRIMARY KEY,
  proveedor_id INT NOT NULL REFERENCES proveedores(id),
  compra_id    INT REFERENCES compras(id),
  monto        NUMERIC(10,2) NOT NULL,
  saldo        NUMERIC(10,2) NOT NULL,
  estado       VARCHAR(15) DEFAULT 'PENDIENTE',
  vencimiento  DATE,
  creado_en    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE empleados (
  id         SERIAL PRIMARY KEY,
  usuario_id INT UNIQUE REFERENCES usuarios(id),
  puesto     VARCHAR(80),
  salario    NUMERIC(10,2) NOT NULL,
  activo     BOOLEAN DEFAULT TRUE
);

CREATE TABLE periodos_nomina (
  id        SERIAL PRIMARY KEY,
  periodo   VARCHAR(30) NOT NULL,   -- 'QUINCENAL 1 - ENE 2026'
  creado_en TIMESTAMP DEFAULT NOW()
);

CREATE TABLE nomina (
  id           SERIAL PRIMARY KEY,
  periodo_id   INT NOT NULL REFERENCES periodos_nomina(id),
  empleado_id  INT NOT NULL REFERENCES empleados(id),
  salario_base NUMERIC(10,2) NOT NULL,
  deducciones  NUMERIC(10,2) DEFAULT 0,
  neto_pagar   NUMERIC(10,2) NOT NULL
);

-- ──────────────────────────────────────────────────────────────
-- SEGURIDAD — Permisos granulares
-- ──────────────────────────────────────────────────────────────

CREATE TABLE permisos (
  id          SERIAL PRIMARY KEY,
  clave       VARCHAR(60) NOT NULL UNIQUE,   -- 'VENTAS_VER', 'PRODUCTOS_EDITAR'
  descripcion VARCHAR(120)
);

CREATE TABLE roles_permisos (
  rol_id     INT NOT NULL REFERENCES roles(id),
  permiso_id INT NOT NULL REFERENCES permisos(id),
  PRIMARY KEY (rol_id, permiso_id)
);

-- ──────────────────────────────────────────────────────────────
-- ÍNDICES
-- ──────────────────────────────────────────────────────────────

CREATE INDEX idx_ventas_turno     ON ventas(turno_id);
CREATE INDEX idx_ventas_cajero    ON ventas(cajero_id);
CREATE INDEX idx_ventas_creado    ON ventas(creado_en);
CREATE INDEX idx_detalle_venta    ON detalle_ventas(venta_id);
CREATE INDEX idx_inv_stock        ON inventario(stock_actual);
CREATE INDEX idx_log_creado       ON log_auditoria(creado_en);
CREATE INDEX idx_mov_producto     ON movimientos_inventario(producto_id);
CREATE INDEX idx_clientes_tipo    ON clientes(tipo);
CREATE INDEX idx_puntos_cliente   ON transacciones_puntos(cliente_id);
CREATE INDEX idx_cxc_estado       ON cuentas_cobrar(estado);
CREATE INDEX idx_cxp_estado       ON cuentas_pagar(estado);

-- ──────────────────────────────────────────────────────────────
-- DATOS SEMILLA (seed)
-- ──────────────────────────────────────────────────────────────

-- Roles
INSERT INTO roles (nombre) VALUES ('ADMIN'), ('CAJERO'), ('SUPERVISOR');

-- Usuarios  (PIN por defecto: 1234)
-- El hash corresponde a bcrypt("1234", cost=10)
-- Si quieres regenerarlo: https://bcrypt-generator.com/
INSERT INTO usuarios (nombre, pin_hash, rol_id) VALUES
  ('Admin',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1),
  ('Cajero 1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 2),
  ('Cajero 2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 2);

-- Métodos de pago
INSERT INTO metodos_pago (nombre) VALUES ('EFECTIVO'), ('TARJETA');

-- Categorías
INSERT INTO categorias (nombre) VALUES
  ('Bebidas'), ('Snacks'), ('Alimentos'), ('Lacteos'), ('Limpieza');

-- Productos
INSERT INTO productos (sku, nombre, categoria_id, precio) VALUES
  ('1001', 'Agua Purificada 1L',   1, 12.00),
  ('1002', 'Refresco Cola 600ml',  1, 18.00),
  ('1003', 'Jugo Naranja 1L',      1, 22.00),
  ('2001', 'Papas Fritas 45g',     2,  9.00),
  ('2002', 'Galletas de Avena',    2, 15.00),
  ('2003', 'Churritos 55g',        2,  8.00),
  ('3001', 'Arroz 1kg',            3, 25.00),
  ('3002', 'Frijol Negro 1kg',     3, 28.00),
  ('4001', 'Leche Entera 1L',      4, 24.00),
  ('4002', 'Yogurt Natural 1kg',   4, 35.00),
  ('5001', 'Jabon de Barra',       5, 18.00),
  ('5002', 'Detergente 1kg',       5, 45.00);

-- Inventario
INSERT INTO inventario (producto_id, stock_actual, stock_minimo) VALUES
  (1,   2, 10),   -- Agua Purificada  <- BAJO STOCK
  (2,  24,  5),
  (3,  18,  5),
  (4,  45, 10),
  (5,   1,  5),   -- Galletas de Avena <- BAJO STOCK
  (6,  30,  8),
  (7,  20, 10),
  (8,  15, 10),
  (9,  30,  8),
  (10, 12,  5),
  (11, 50, 10),
  (12, 18,  5);

-- Clientes demo
INSERT INTO clientes (codigo, nombre, rfc, tipo, puntos_acumulados) VALUES
  ('C-001', 'Restaurante Los Arcos', 'REST800101ABC', 'MAYORISTA', 0),
  ('C-002', 'Maria Garcia Lopez',    'GALM850203XYZ', 'MENUDEO',  320),
  ('C-003', 'Tienda El Sol',         'TIES901112DEF', 'MAYORISTA', 0),
  ('C-004', 'Pedro Martinez Ruiz',   'MARP780530GHI', 'MENUDEO',  85);
