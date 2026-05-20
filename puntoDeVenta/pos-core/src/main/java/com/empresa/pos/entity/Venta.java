package com.empresa.pos.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Venta {
    private UUID uuid;
    private String folio;
    private UUID sucursalId;
    private UUID cajeroId;
    private UUID turnoId;
    private UUID clienteId;
    private LocalDateTime fecha;
    private String estado;
    private BigDecimal subtotal;
    private BigDecimal ivaTotal;
    private BigDecimal iepsTotal;
    private BigDecimal total;
    private String cfdiUuid;
    private String syncStatus;

    public Venta() {
        this.syncStatus = "PENDING";
        this.fecha = LocalDateTime.now();
    }

    // Getters and Setters omitted for brevity...
}
