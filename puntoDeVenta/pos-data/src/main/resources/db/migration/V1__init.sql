-- V1__init.sql: Initial database schema for POS Empresarial ERP
-- Compatible with SQLite and PostgreSQL

CREATE TABLE CATEGORIA (
    uuid VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    padre_id VARCHAR(36),
    FOREIGN KEY (padre_id) REFERENCES CATEGORIA(uuid)
);

CREATE TABLE PROVEEDOR (
    uuid VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    rfc VARCHAR(13) NOT NULL
);

CREATE TABLE PRODUCTO (
    uuid VARCHAR(36) PRIMARY KEY,
    sku VARCHAR(50) UNIQUE NOT NULL,
    descripcion TEXT NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    unidad_medida VARCHAR(36),
    clave_sat VARCHAR(36),
    iva_incluido BOOLEAN NOT NULL DEFAULT 1,
    ieps_porcentaje DECIMAL(5,2) DEFAULT 0,
    imagen_path TEXT,
    stock_minimo INTEGER DEFAULT 0,
    es_pesable BOOLEAN NOT NULL DEFAULT 0,
    categoria_id VARCHAR(36),
    proveedor_id VARCHAR(36),
    FOREIGN KEY (categoria_id) REFERENCES CATEGORIA(uuid),
    FOREIGN KEY (proveedor_id) REFERENCES PROVEEDOR(uuid)
);

CREATE TABLE ALMACEN (
    uuid VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    sucursal_id VARCHAR(36) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT 1
);

CREATE TABLE STOCK (
    uuid VARCHAR(36) PRIMARY KEY,
    producto_id VARCHAR(36) NOT NULL,
    almacen_id VARCHAR(36) NOT NULL,
    cantidad DECIMAL(10,3) NOT NULL DEFAULT 0,
    costo_promedio DECIMAL(10,2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (producto_id) REFERENCES PRODUCTO(uuid),
    FOREIGN KEY (almacen_id) REFERENCES ALMACEN(uuid)
);

CREATE TABLE TURNO_CAJA (
    uuid VARCHAR(36) PRIMARY KEY,
    id_visible VARCHAR(50) NOT NULL,
    cajero_id VARCHAR(36) NOT NULL,
    sucursal_id VARCHAR(36) NOT NULL,
    apertura TIMESTAMP NOT NULL,
    cierre TIMESTAMP,
    fondo_inicial DECIMAL(10,2) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL,
    total_efectivo DECIMAL(10,2) DEFAULT 0,
    total_tarjeta DECIMAL(10,2) DEFAULT 0,
    total_ventas DECIMAL(10,2) DEFAULT 0
);

CREATE TABLE CORTE_CAJA (
    uuid VARCHAR(36) PRIMARY KEY,
    turno_id VARCHAR(36) NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    efectivo_contado DECIMAL(10,2) NOT NULL DEFAULT 0,
    efectivo_sistema DECIMAL(10,2) NOT NULL DEFAULT 0,
    diferencia DECIMAL(10,2) NOT NULL DEFAULT 0,
    denominaciones TEXT, -- JSON
    pin_cajero VARCHAR(255) NOT NULL,
    pin_supervisor VARCHAR(255),
    generado_at TIMESTAMP NOT NULL,
    FOREIGN KEY (turno_id) REFERENCES TURNO_CAJA(uuid)
);

CREATE TABLE CLIENTE_FISCAL (
    uuid VARCHAR(36) PRIMARY KEY,
    rfc VARCHAR(13) NOT NULL,
    razon_social VARCHAR(255) NOT NULL,
    cp_fiscal CHAR(5) NOT NULL,
    regimen_fiscal VARCHAR(36),
    uso_cfdi_default VARCHAR(36),
    tipo VARCHAR(50) NOT NULL,
    puntos_lealtad INTEGER DEFAULT 0,
    email VARCHAR(255)
);

CREATE TABLE VENTA (
    uuid VARCHAR(36) PRIMARY KEY,
    folio VARCHAR(20) NOT NULL,
    sucursal_id VARCHAR(36) NOT NULL,
    cajero_id VARCHAR(36) NOT NULL,
    turno_id VARCHAR(36) NOT NULL,
    cliente_id VARCHAR(36),
    fecha TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0,
    iva_total DECIMAL(10,2) NOT NULL DEFAULT 0,
    ieps_total DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL DEFAULT 0,
    cfdi_uuid TEXT,
    sync_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (turno_id) REFERENCES TURNO_CAJA(uuid),
    FOREIGN KEY (cliente_id) REFERENCES CLIENTE_FISCAL(uuid)
);

CREATE TABLE DETALLE_VENTA (
    uuid VARCHAR(36) PRIMARY KEY,
    venta_id VARCHAR(36) NOT NULL,
    producto_id VARCHAR(36) NOT NULL,
    cantidad DECIMAL(10,3) NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    descuento_pct DECIMAL(5,2) DEFAULT 0,
    iva_monto DECIMAL(10,2) NOT NULL DEFAULT 0,
    ieps_monto DECIMAL(10,2) NOT NULL DEFAULT 0,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (venta_id) REFERENCES VENTA(uuid),
    FOREIGN KEY (producto_id) REFERENCES PRODUCTO(uuid)
);

CREATE TABLE PAGO (
    uuid VARCHAR(36) PRIMARY KEY,
    venta_id VARCHAR(36) NOT NULL,
    metodo VARCHAR(50) NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    referencia VARCHAR(255),
    FOREIGN KEY (venta_id) REFERENCES VENTA(uuid)
);

CREATE TABLE CFDI (
    uuid VARCHAR(36) PRIMARY KEY,
    venta_id VARCHAR(36) NOT NULL,
    uuid_sat VARCHAR(36),
    folio VARCHAR(50),
    serie VARCHAR(20),
    estado VARCHAR(20) NOT NULL,
    xml_timbrado TEXT,
    qr_data TEXT,
    fecha_timbrado TIMESTAMP,
    pac_response TEXT, -- JSON
    FOREIGN KEY (venta_id) REFERENCES VENTA(uuid)
);

CREATE TABLE SYNC_QUEUE (
    id VARCHAR(36) PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(36) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    payload TEXT NOT NULL, -- JSON
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts INTEGER DEFAULT 0,
    last_error TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE LOG_AUDITORIA (
    id VARCHAR(36) PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id VARCHAR(36) NOT NULL,
    autorizante_id VARCHAR(36),
    accion VARCHAR(100) NOT NULL,
    entidad_tipo VARCHAR(50) NOT NULL,
    entidad_id VARCHAR(36) NOT NULL,
    valor_anterior TEXT, -- JSON
    valor_nuevo TEXT, -- JSON
    sucursal_id VARCHAR(36) NOT NULL,
    ip_terminal VARCHAR(50)
);

CREATE INDEX idx_log_auditoria_timestamp ON LOG_AUDITORIA(timestamp);
CREATE INDEX idx_sync_queue_status ON SYNC_QUEUE(status);
CREATE INDEX idx_venta_turno ON VENTA(turno_id);
