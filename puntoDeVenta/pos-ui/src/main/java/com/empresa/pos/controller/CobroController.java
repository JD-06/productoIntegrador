package com.empresa.pos.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.math.BigDecimal;

public class CobroController {

    @FXML private Label lblTotalCobro;
    @FXML private ToggleButton btnEfectivo;
    @FXML private ToggleButton btnTarjeta;
    @FXML private VBox panelEfectivo;
    @FXML private TextField txtMonto;
    @FXML private Label lblCambio;

    private BigDecimal total = BigDecimal.ZERO;
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

    public void setOnConfirmar(Runnable callback) {
        this.onConfirmar = callback;
    }

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
        if (btnEfectivo.isSelected()) {
            try {
                BigDecimal monto = new BigDecimal(txtMonto.getText().replace(",", ""));
                if (monto.compareTo(total) < 0) {
                    lblCambio.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #C62828;");
                    lblCambio.setText("Monto insuficiente");
                    return;
                }
            } catch (NumberFormatException e) {
                return;
            }
        }
        if (onConfirmar != null) onConfirmar.run();
        cerrar();
    }

    @FXML
    private void handleCancelar() {
        cerrar();
    }

    private void cerrar() {
        ((Stage) lblTotalCobro.getScene().getWindow()).close();
    }
}
