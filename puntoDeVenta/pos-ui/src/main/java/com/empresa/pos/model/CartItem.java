package com.empresa.pos.model;

import javafx.beans.property.*;
import java.math.BigDecimal;

public class CartItem {

    private final int dbProductoId;
    private final StringProperty producto = new SimpleStringProperty();
    private final DoubleProperty cantidad  = new SimpleDoubleProperty();
    private final ObjectProperty<BigDecimal> precio   = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> subtotal = new SimpleObjectProperty<>();

    public CartItem(int dbProductoId, String nombre, double cantidad, BigDecimal precio) {
        this.dbProductoId = dbProductoId;
        this.producto.set(nombre);
        this.cantidad.set(cantidad);
        this.precio.set(precio);
        calcularSubtotal();
        this.cantidad.addListener((obs, o, n) -> calcularSubtotal());
    }

    private void calcularSubtotal() {
        this.subtotal.set(this.precio.get().multiply(BigDecimal.valueOf(this.cantidad.get())));
    }

    public int getDbProductoId() { return dbProductoId; }

    public String getProducto()               { return producto.get(); }
    public StringProperty productoProperty()  { return producto; }

    public double getCantidad()               { return cantidad.get(); }
    public void   setCantidad(double v)       { cantidad.set(v); }
    public DoubleProperty cantidadProperty()  { return cantidad; }

    public BigDecimal getPrecio()                       { return precio.get(); }
    public ObjectProperty<BigDecimal> precioProperty()  { return precio; }

    public BigDecimal getSubtotal()                       { return subtotal.get(); }
    public ObjectProperty<BigDecimal> subtotalProperty()  { return subtotal; }
}
