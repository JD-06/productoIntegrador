package com.empresa.pos.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.*;
import java.math.BigDecimal;

public class POSViewModel {
    private final ObservableList<CartItem> items = FXCollections.observableArrayList();
    private final ObjectProperty<BigDecimal> subtotal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> iva = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> total = new SimpleObjectProperty<>(BigDecimal.ZERO);

    public POSViewModel() {
        items.addListener((javafx.collections.ListChangeListener<CartItem>) c -> calculateTotals());
    }

    public void addItem(CartItem item) {
        items.add(item);
        calculateTotals();
    }

    private void calculateTotals() {
        BigDecimal sum = items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        subtotal.set(sum);
        iva.set(sum.multiply(new BigDecimal("0.16")));
        total.set(subtotal.get().add(iva.get()));
    }

    public ObservableList<CartItem> getItems() { return items; }
    public ObjectProperty<BigDecimal> subtotalProperty() { return subtotal; }
    public ObjectProperty<BigDecimal> ivaProperty() { return iva; }
    public ObjectProperty<BigDecimal> totalProperty() { return total; }
}
