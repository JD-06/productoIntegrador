package com.empresa.pos.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;
import java.math.BigDecimal;
import javafx.fxml.Initializable;

public class POSController implements Initializable {

    @FXML private TextField txtBusqueda;
    @FXML private TableView<CartItem> tblCarrito;
    @FXML private TableColumn<CartItem, String> colProducto;
    @FXML private TableColumn<CartItem, Double> colCantidad;
    @FXML private TableColumn<CartItem, BigDecimal> colPrecio;
    @FXML private TableColumn<CartItem, BigDecimal> colSubtotal;
    
    @FXML private Label lblTotal;
    @FXML private Label lblSubtotal;
    @FXML private Label lblIva;
    @FXML private Label lblUsuario;
    @FXML private Label lblTurno;
    @FXML private Label lblStatus;

    private POSViewModel viewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        viewModel = new POSViewModel();
        setupTable();
        setupBindings();
        setupEvents();
    }

    private void setupTable() {
        colProducto.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        
        tblCarrito.setItems(viewModel.getItems());
    }

    private void setupBindings() {
        lblSubtotal.textProperty().bind(viewModel.subtotalProperty().asString("$%.2f"));
        lblIva.textProperty().bind(viewModel.ivaProperty().asString("$%.2f"));
        lblTotal.textProperty().bind(viewModel.totalProperty().asString("$%.2f"));
    }

    private void setupEvents() {
        txtBusqueda.setOnAction(event -> {
            String sku = txtBusqueda.getText();
            if (!sku.isEmpty()) {
                // Simulación de búsqueda de producto
                handleAgregarProducto(sku);
                txtBusqueda.clear();
            }
        });
    }

    private void handleAgregarProducto(String sku) {
        // Simulación: en el futuro esto consultará a pos-data/pos-core
        CartItem item = new CartItem(UUID.randomUUID(), "Producto " + sku, 1.0, new BigDecimal("25.00"));
        viewModel.addItem(item);
    }

    @FXML
    private void handleCobrar() {
        // Lógica de cobro (Sprint 06)
        System.out.println("Procesando cobro de: " + lblTotal.getText());
    }
}
