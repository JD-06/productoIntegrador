package com.empresa.pos.service;

import java.math.BigDecimal;
import java.util.Collection;

public class CartService {

    private static final BigDecimal IVA_RATE = new BigDecimal("0.16");

    public BigDecimal calcularSubtotal(Collection<BigDecimal> subtotales) {
        return subtotales.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularIva(BigDecimal subtotal) {
        return subtotal.multiply(IVA_RATE);
    }

    public BigDecimal calcularTotal(BigDecimal subtotal) {
        return subtotal.add(calcularIva(subtotal));
    }
}
