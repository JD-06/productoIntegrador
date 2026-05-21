package com.empresa.pos.controlador;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.InventarioDAO;
import com.empresa.pos.dao.VentaDAO;
import com.empresa.pos.modelo.CartItem;
import com.empresa.pos.servicio.TicketService;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.List;

public class CobroController {

    private static final Logger log = LoggerFactory.getLogger(CobroController.class);

    @FXML private Label lblTotalCobro;
    @FXML private ToggleButton btnEfectivo;
    @FXML private ToggleButton btnTarjeta;
    @FXML private VBox panelEfectivo;
    @FXML private TextField txtMonto;
    @FXML private Label lblCambio;

    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal iva = BigDecimal.ZERO;
    private ObservableList<CartItem> items;
    private int turnoId;
    private int cajeroId;
    private Runnable onConfirmar;

    @FXML
    public void initialize() {
        ToggleGroup grupo = new ToggleGroup();
        btnEfectivo.setToggleGroup(grupo);
        btnTarjeta.setToggleGroup(grupo);
        btnEfectivo.setSelected(true);

        grupo.selectedToggleProperty().addListener((obs, old, newVal) -> {
            if (newVal == null) old.setSelected(true);
        });

        txtMonto.textProperty().addListener((obs, old, val) -> calcularCambio(val));
        txtMonto.setOnAction(e -> handleConfirmar());
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
        lblTotalCobro.setText(String.format("$%.2f", total));
    }

    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public void setIva(BigDecimal iva) { this.iva = iva; }
    public void setItems(ObservableList<CartItem> items) { this.items = items; }
    public void setTurnoId(int turnoId) { this.turnoId = turnoId; }
    public void setCajeroId(int cajeroId) { this.cajeroId = cajeroId; }
    public void setOnConfirmar(Runnable callback) { this.onConfirmar = callback; }

    @FXML
    private void handleMetodoPago() {
        boolean esEfectivo = btnEfectivo.isSelected();
        panelEfectivo.setVisible(esEfectivo);
        panelEfectivo.setManaged(esEfectivo);
        if (esEfectivo) txtMonto.requestFocus();
    }

    private void calcularCambio(String montoStr) {
        try {
            BigDecimal monto = new BigDecimal(montoStr.replace(",", ""));
            BigDecimal cambio = monto.subtract(total);
            boolean suficiente = cambio.compareTo(BigDecimal.ZERO) >= 0;
            lblCambio.setText(String.format("$%.2f", cambio.abs()));
            lblCambio.setStyle(suficiente
                ? "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;"
                : "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #C62828;");
        } catch (NumberFormatException e) {
            lblCambio.setText("$0.00");
        }
    }

    @FXML
    private void handleConfirmar() {
        BigDecimal montoRecibido = total;
        BigDecimal cambio = BigDecimal.ZERO;

        if (btnEfectivo.isSelected()) {
            try {
                BigDecimal monto = new BigDecimal(txtMonto.getText().replace(",", ""));
                if (monto.compareTo(total) < 0) {
                    lblCambio.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #C62828;");
                    lblCambio.setText("Monto insuficiente");
                    return;
                }
                montoRecibido = monto;
                cambio = monto.subtract(total);
            } catch (NumberFormatException e) {
                return;
            }
        }

        String metodoNombre = btnEfectivo.isSelected() ? "EFECTIVO" : "TARJETA";
        int metodoPagoId = AppContext.getInstance().ventaDAO().getMetodoPagoId(metodoNombre);
        int ventaId = guardarVentaEnBD(metodoPagoId, montoRecibido, cambio);
        if (ventaId > 0) {
            actualizarStock();
            generarTickets(ventaId);
            if (onConfirmar != null) onConfirmar.run();
            cerrar();
        }
    }

    private int guardarVentaEnBD(int metodoPagoId, BigDecimal montoRecibido, BigDecimal cambio) {
        try {
            VentaDAO.Venta v = new VentaDAO.Venta();
            v.setTurnoId(turnoId);
            v.setCajeroId(cajeroId);
            v.setClienteId(0);
            v.setSubtotal(subtotal);
            v.setIva(iva);
            v.setTotal(total);
            v.setMetodoPagoId(metodoPagoId);
            v.setMontoRecibido(montoRecibido);
            v.setCambio(cambio);
            v.setEstado("COMPLETADA");

            List<com.empresa.pos.dao.VentaDAO.DetalleVenta> detalles = items.stream()
                    .map(i -> new com.empresa.pos.dao.VentaDAO.DetalleVenta(
                            i.getDbProductoId(), (int) i.getCantidad(), i.getPrecio(), i.getSubtotal()))
                    .toList();
            return AppContext.getInstance().ventaDAO().insertConDetalle(v, detalles);
        } catch (Exception e) {
            log.error("Error al guardar venta", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error al guardar la venta.");
            alert.showAndWait();
            return 0;
        }
    }

    private void actualizarStock() {
        InventarioDAO dao = AppContext.getInstance().inventarioDAO();
        for (CartItem item : items) {
            dao.actualizarStock(item.getDbProductoId(), (int) item.getCantidad(), "SALIDA");
        }
    }

    private void generarTickets(int ventaId) {
        try {
            String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String baseName = "ticket_" + ventaId + "_" + fecha;

            // TXT
            String ticketTxt = TicketService.generarTicketTxt(ventaId, items, subtotal, iva, total, turnoId, cajeroId);
            Path txtPath = Files.writeString(Path.of(System.getProperty("user.home"), "Documents", baseName + ".txt"), ticketTxt);

            // PDF
            byte[] ticketPdf = TicketService.generarTicketPdf(ventaId, items, subtotal, iva, total, turnoId, cajeroId);
            Path pdfPath = Files.write(Path.of(System.getProperty("user.home"), "Documents", baseName + ".html"), ticketPdf);

            log.info("Tickets generados: {} y {}", txtPath, pdfPath);
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(pdfPath.toFile());
                }
            } catch (Exception ex) {
                log.warn("No se pudo abrir el ticket PDF automaticamente: {}", ex.getMessage());
            }
        } catch (IOException e) {
            log.error("Error generando tickets", e);
        }
    }

    @FXML
    private void handleCancelar() {
        cerrar();
    }

    private void cerrar() {
        ((Stage) lblTotalCobro.getScene().getWindow()).close();
    }
}
