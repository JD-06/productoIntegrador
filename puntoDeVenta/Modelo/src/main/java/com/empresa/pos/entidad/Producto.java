package com.empresa.pos.entidad;

import java.math.BigDecimal;
import java.util.UUID;

public class Producto {
    private UUID uuid;
    private String sku;
    private String descripcion;
    private BigDecimal precio;
    private UUID unidadMedidaId;
    private String claveSat;
    private boolean ivaIncluido;
    private BigDecimal iepsPorcentaje;
    private String imagenPath;
    private int stockMinimo;
    private boolean esPesable;
    private UUID categoriaId;
    private UUID proveedorId;

    // Constructors, Getters, and Setters

    public Producto() {}

    public Producto(UUID uuid, String sku, String descripcion, BigDecimal precio) {
        this.uuid = uuid;
        this.sku = sku;
        this.descripcion = descripcion;
        this.precio = precio;
        this.ivaIncluido = true;
        this.iepsPorcentaje = BigDecimal.ZERO;
        this.esPesable = false;
        this.stockMinimo = 0;
    }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public UUID getUnidadMedidaId() { return unidadMedidaId; }
    public void setUnidadMedidaId(UUID unidadMedidaId) { this.unidadMedidaId = unidadMedidaId; }

    public String getClaveSat() { return claveSat; }
    public void setClaveSat(String claveSat) { this.claveSat = claveSat; }

    public boolean isIvaIncluido() { return ivaIncluido; }
    public void setIvaIncluido(boolean ivaIncluido) { this.ivaIncluido = ivaIncluido; }

    public BigDecimal getIepsPorcentaje() { return iepsPorcentaje; }
    public void setIepsPorcentaje(BigDecimal iepsPorcentaje) { this.iepsPorcentaje = iepsPorcentaje; }

    public String getImagenPath() { return imagenPath; }
    public void setImagenPath(String imagenPath) { this.imagenPath = imagenPath; }

    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    public boolean isEsPesable() { return esPesable; }
    public void setEsPesable(boolean esPesable) { this.esPesable = esPesable; }

    public UUID getCategoriaId() { return categoriaId; }
    public void setCategoriaId(UUID categoriaId) { this.categoriaId = categoriaId; }

    public UUID getProveedorId() { return proveedorId; }
    public void setProveedorId(UUID proveedorId) { this.proveedorId = proveedorId; }
}
