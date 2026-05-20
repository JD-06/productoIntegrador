package com.empresa.pos.ui;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.util.UUID;

public class CartItem {
    private final StringProperty producto = new SimpleStringProperty();
    private final DoubleProperty cantidad = new SimpleDoubleProperty();
    private final ObjectProperty<BigDecimal> precio = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> subtotal = new SimpleObjectProperty<>();
    private final UUID productoId;

    public CartItem(UUID productoId, String producto, double cantidad, BigDecimal precio) {
        this.productoId = productoId;
        this.producto.set(producto);
        this.cantidad.set(cantidad);
        this.precio.set(precio);
        calculateSubtotal();
        
        this.cantidad.addListener((obs, oldVal, newVal) -> calculateSubtotal());
    }

    private void calculateSubtotal() {
        this.subtotal.set(this.precio.get().multiply(BigDecimal.valueOf(this.cantidad.get())));
    }

    public String getProducto() { return producto.get(); }
    public StringProperty productoProperty() { return producto; }

    public double getCantidad() { return cantidad.get(); }
    public DoubleProperty cantidadProperty() { return cantidad; }

    public BigDecimal getPrecio() { return precio.get(); }
    public ObjectProperty<BigDecimal> precioProperty() { return precio; }

    public BigDecimal getSubtotal() { return subtotal.get(); }
    public ObjectProperty<BigDecimal> subtotalProperty() { return subtotal; }

    public UUID getProductoId() { return productoId; }
}
