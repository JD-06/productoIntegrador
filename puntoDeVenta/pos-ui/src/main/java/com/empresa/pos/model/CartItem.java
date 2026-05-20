package com.empresa.pos.model;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.util.UUID;

public class CartItem {

    private final UUID productoId;
    private final StringProperty producto = new SimpleStringProperty();
    private final DoubleProperty cantidad = new SimpleDoubleProperty();
    private final ObjectProperty<BigDecimal> precio = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> subtotal = new SimpleObjectProperty<>();

    public CartItem(UUID productoId, String producto, double cantidad, BigDecimal precio) {
        this.productoId = productoId;
        this.producto.set(producto);
        this.cantidad.set(cantidad);
        this.precio.set(precio);
        calcularSubtotal();
        this.cantidad.addListener((obs, oldVal, newVal) -> calcularSubtotal());
    }

    private void calcularSubtotal() {
        this.subtotal.set(this.precio.get().multiply(BigDecimal.valueOf(this.cantidad.get())));
    }

    public UUID getProductoId() { return productoId; }

    public String getProducto() { return producto.get(); }
    public StringProperty productoProperty() { return producto; }

    public double getCantidad() { return cantidad.get(); }
    public DoubleProperty cantidadProperty() { return cantidad; }

    public BigDecimal getPrecio() { return precio.get(); }
    public ObjectProperty<BigDecimal> precioProperty() { return precio; }

    public BigDecimal getSubtotal() { return subtotal.get(); }
    public ObjectProperty<BigDecimal> subtotalProperty() { return subtotal; }
}
