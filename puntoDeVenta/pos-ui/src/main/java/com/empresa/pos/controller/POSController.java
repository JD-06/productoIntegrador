package com.empresa.pos.controller;

import com.empresa.pos.model.CartItem;
import com.empresa.pos.service.CartService;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

public class POSController implements Initializable {

    // --- View ---
    @FXML private TextField txtBusqueda;
    @FXML private TableView<CartItem> tblCarrito;
    @FXML private TableColumn<CartItem, String>     colProducto;
    @FXML private TableColumn<CartItem, Double>     colCantidad;
    @FXML private TableColumn<CartItem, BigDecimal> colPrecio;
    @FXML private TableColumn<CartItem, BigDecimal> colSubtotal;
    @FXML private Label lblSubtotal;
    @FXML private Label lblIva;
    @FXML private Label lblTotal;
    @FXML private Label lblUsuario;
    @FXML private Label lblTurno;
    @FXML private Label lblStatus;

    // --- Model ---
    private final ObservableList<CartItem> items    = FXCollections.observableArrayList();
    private final ObjectProperty<BigDecimal> subtotal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> iva      = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> total    = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // --- Service ---
    private final CartService cartService = new CartService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTabla();
        configurarBindings();
        configurarEventos();
        items.addListener((javafx.collections.ListChangeListener<CartItem>) c -> recalcular());
        Platform.runLater(this::configurarAtajos);
    }

    private void configurarTabla() {
        colProducto.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        tblCarrito.setItems(items);
    }

    private void configurarBindings() {
        lblSubtotal.textProperty().bind(subtotal.asString("%.2f"));
        lblIva.textProperty().bind(iva.asString("%.2f"));
        lblTotal.textProperty().bind(total.asString("%.2f"));
    }

    private void configurarEventos() {
        txtBusqueda.setOnAction(event -> {
            String sku = txtBusqueda.getText().trim();
            if (!sku.isEmpty()) {
                agregarProducto(sku);
                txtBusqueda.clear();
            }
        });
    }

    private void configurarAtajos() {
        Scene scene = tblCarrito.getScene();
        if (scene == null) return;
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case F12 -> handleCobrar();
                case F11 -> eliminarItemSeleccionado();
                case F5  -> txtBusqueda.requestFocus();
                case F4  -> handleCajon();
                default  -> { }
            }
        });
    }

    private void agregarProducto(String sku) {
        CartItem item = new CartItem(UUID.randomUUID(), "Producto " + sku, 1.0, new BigDecimal("25.00"));
        items.add(item);
    }

    private void recalcular() {
        BigDecimal sub = cartService.calcularSubtotal(
            items.stream().map(CartItem::getSubtotal).collect(Collectors.toList())
        );
        subtotal.set(sub);
        iva.set(cartService.calcularIva(sub));
        total.set(cartService.calcularTotal(sub));
    }

    private void eliminarItemSeleccionado() {
        CartItem sel = tblCarrito.getSelectionModel().getSelectedItem();
        if (sel != null) items.remove(sel);
    }

    private void handleCajon() {
        System.out.println("[F4] Abrir cajon de dinero");
    }

    @FXML
    private void handleCobrar() {
        if (items.isEmpty()) return;
        try {
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initOwner(tblCarrito.getScene().getWindow());
            modal.setTitle("Cobro");
            modal.setResizable(false);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cobro_modal.fxml"));
            Parent root = loader.load();

            CobroController ctrl = loader.getController();
            ctrl.setTotal(total.get());
            ctrl.setOnConfirmar(() -> {
                items.clear();
                recalcular();
            });

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/design-system.css").toExternalForm());
            modal.setScene(scene);
            modal.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
