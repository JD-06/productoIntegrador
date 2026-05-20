package com.empresa.pos.controller;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.CategoriaDAO;
import com.empresa.pos.dao.InventarioDAO;
import com.empresa.pos.dao.ProductoDAO;
import com.empresa.pos.dao.VentaDAO;
import com.empresa.pos.model.CartItem;
import com.empresa.pos.service.CartService;
import com.empresa.pos.service.TicketService;
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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class POSController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(POSController.class);

    // --- View ---
    @FXML private TextField txtBusqueda;
    @FXML private TextField txtBusquedaProducto;
    @FXML private ComboBox<CategoriaDAO.Categoria> cmbCategoriaPos;
    @FXML private FlowPane flowProductos;
    @FXML private TableView<CartItem> tblCarrito;
    @FXML private TableColumn<CartItem, String>     colProducto;
    @FXML private TableColumn<CartItem, Double>     colCantidad;
    @FXML private TableColumn<CartItem, BigDecimal> colPrecio;
    @FXML private TableColumn<CartItem, BigDecimal> colSubtotal;
    @FXML private TableColumn<CartItem, Void>       colAccion;
    @FXML private Label lblSubtotal;
    @FXML private Label lblIva;
    @FXML private Label lblTotal;
    @FXML private Label lblUsuario;
    @FXML private Label lblTurno;
    @FXML private Label lblStatus;
    @FXML private Label lblItemsCarrito;

    // --- Model ---
    private final ObservableList<CartItem> items = FXCollections.observableArrayList();
    private final ObjectProperty<BigDecimal> subtotal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> iva      = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> total    = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // --- Service ---
    private final CartService cartService = new CartService();

    // --- Context ---
    private int cajeroId;
    private String cajeroNombre;
    private int turnoId;
    private List<ProductoDAO.Producto> productosDisponibles;

    public void setCajero(int id, String nombre) {
        this.cajeroId = id;
        this.cajeroNombre = nombre;
        Platform.runLater(() -> lblUsuario.setText("Cajero: " + nombre));
    }

    public void setTurno(int turnoId) {
        this.turnoId = turnoId;
        Platform.runLater(() -> lblTurno.setText("Turno: #" + turnoId));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTabla();
        configurarBindings();
        configurarEventos();
        cargarCategorias();
        cargarProductos();
        items.addListener((javafx.collections.ListChangeListener<CartItem>) c -> recalcular());
        Platform.runLater(this::configurarAtajos);
    }

    private void configurarTabla() {
        colProducto.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        colAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("X");
            {
                btn.setStyle("-fx-background-color: #C62828; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 6;");
                btn.setOnAction(e -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    items.remove(item);
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tblCarrito.setItems(items);
    }

    private void configurarBindings() {
        lblSubtotal.textProperty().bind(subtotal.asString("%.2f"));
        lblIva.textProperty().bind(iva.asString("%.2f"));
        lblTotal.textProperty().bind(total.asString("%.2f"));
        items.addListener((javafx.collections.ListChangeListener<CartItem>) c -> {
            lblItemsCarrito.setText(items.size() + " items");
        });
    }

    private void configurarEventos() {
        txtBusqueda.setOnAction(event -> buscarPorSku(txtBusqueda.getText().trim()));
        txtBusquedaProducto.setOnAction(event -> filtrarProductos());
        cmbCategoriaPos.setOnAction(event -> filtrarProductos());
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

    private void cargarCategorias() {
        try {
            List<CategoriaDAO.Categoria> cats = AppContext.getInstance().categoriaDAO().findAll();
            CategoriaDAO.Categoria todas = new CategoriaDAO.Categoria();
            todas.setId(0);
            todas.setNombre("Todas");
            cmbCategoriaPos.getItems().add(todas);
            cmbCategoriaPos.getItems().addAll(cats);
            cmbCategoriaPos.getSelectionModel().selectFirst();
        } catch (Exception e) {
            log.warn("No se pudieron cargar categorias: {}", e.getMessage());
        }
    }

    private void cargarProductos() {
        try {
            productosDisponibles = AppContext.getInstance().productoDAO().findAll();
            renderizarProductos(productosDisponibles);
        } catch (Exception e) {
            log.error("Error al cargar productos", e);
            lblStatus.setText("Error cargando productos");
        }
    }

    private void filtrarProductos() {
        if (productosDisponibles == null) return;
        String texto = txtBusquedaProducto.getText().trim().toLowerCase();
        CategoriaDAO.Categoria cat = cmbCategoriaPos.getSelectionModel().getSelectedItem();
        int catId = cat != null ? cat.getId() : 0;

        List<ProductoDAO.Producto> filtrados = productosDisponibles.stream()
                .filter(p -> {
                    boolean matchText = texto.isEmpty()
                            || p.getNombre().toLowerCase().contains(texto)
                            || p.getSku().toLowerCase().contains(texto);
                    boolean matchCat = catId == 0 || p.getCategoriaId() == catId;
                    return matchText && matchCat && p.isActivo();
                })
                .collect(Collectors.toList());
        renderizarProductos(filtrados);
    }

    private void renderizarProductos(List<ProductoDAO.Producto> lista) {
        flowProductos.getChildren().clear();
        for (ProductoDAO.Producto p : lista) {
            VBox card = new VBox(4);
            card.setStyle("-fx-background-color: white; -fx-padding: 8; -fx-background-radius: 6; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4,0,0,1); -fx-cursor: hand;");
            card.setPrefWidth(130);
            card.setPrefHeight(100);
            card.setAlignment(javafx.geometry.Pos.CENTER);

            Label lblNombre = new Label(p.getNombre());
            lblNombre.setWrapText(true);
            lblNombre.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
            lblNombre.setMaxWidth(110);

            Label lblPrecio = new Label(String.format("$%.2f", p.getPrecio()));
            lblPrecio.setStyle("-fx-font-size: 13px; -fx-text-fill: #1A2D5A;");

            Label lblStock = new Label("Stock: " + p.getStockActual());
            lblStock.setStyle(p.getStockActual() <= p.getStockMinimo()
                    ? "-fx-font-size: 10px; -fx-text-fill: #C62828;"
                    : "-fx-font-size: 10px; -fx-text-fill: #757575;");

            card.getChildren().addAll(lblNombre, lblPrecio, lblStock);
            card.setOnMouseClicked(e -> agregarProductoDesdeCatalogo(p));
            flowProductos.getChildren().add(card);
        }
    }

    private void buscarPorSku(String texto) {
        if (texto.isEmpty()) return;
        try {
            List<ProductoDAO.Producto> resultados = AppContext.getInstance().productoDAO().search(texto);
            if (resultados.isEmpty()) {
                lblStatus.setText("Producto no encontrado: " + texto);
                return;
            }
            ProductoDAO.Producto p = resultados.get(0);
            agregarProductoDesdeCatalogo(p);
            txtBusqueda.clear();
            lblStatus.setText("");
        } catch (Exception e) {
            log.error("Error buscando producto", e);
        }
    }

    private void agregarProductoDesdeCatalogo(ProductoDAO.Producto p) {
        if (p.getStockActual() <= 0) {
            lblStatus.setText("Sin stock: " + p.getNombre());
            return;
        }
        CartItem existente = items.stream()
                .filter(i -> i.getDbProductoId() == p.getId())
                .findFirst().orElse(null);
        if (existente != null) {
            double nuevaCantidad = existente.getCantidad() + 1;
            if (nuevaCantidad > p.getStockActual()) {
                lblStatus.setText("Stock insuficiente: " + p.getNombre());
                return;
            }
            existente.setCantidad(nuevaCantidad);
            recalcular();
        } else {
            CartItem item = new CartItem(p.getId(), p.getNombre(), 1.0, p.getPrecio());
            items.add(item);
        }
        lblStatus.setText("");
    }

    private void recalcular() {
        BigDecimal sub = cartService.calcularSubtotal(
            items.stream().map(CartItem::getSubtotal).collect(Collectors.toList())
        );
        subtotal.set(sub);
        iva.set(cartService.calcularIva(sub));
        total.set(cartService.calcularTotal(sub));
        lblItemsCarrito.setText(items.size() + " items");
    }

    private void eliminarItemSeleccionado() {
        CartItem sel = tblCarrito.getSelectionModel().getSelectedItem();
        if (sel != null) items.remove(sel);
    }

    @FXML
    private void handleEliminarItem() {
        eliminarItemSeleccionado();
    }

    @FXML
    private void handleCancelarVenta() {
        if (items.isEmpty()) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Cancelar la venta actual?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Cancelar venta");
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            items.clear();
            recalcular();
        }
    }

    @FXML
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
            ctrl.setSubtotal(subtotal.get());
            ctrl.setIva(iva.get());
            ctrl.setItems(FXCollections.observableArrayList(items));
            ctrl.setTurnoId(turnoId);
            ctrl.setCajeroId(cajeroId);
            ctrl.setOnConfirmar(() -> {
                items.clear();
                recalcular();
                cargarProductos();
            });

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/design-system.css").toExternalForm());
            modal.setScene(scene);
            modal.showAndWait();
        } catch (IOException e) {
            log.error("Error al abrir modal de cobro", e);
        }
    }

    @FXML
    private void handleExportarCsv() {
        try {
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path dir = Path.of(System.getProperty("user.home"), "Documents", "pos_export_" + stamp);
            Files.createDirectories(dir);

            List<VentaDAO.Venta> ventas = AppContext.getInstance().ventaDAO().findAll();
            List<ProductoDAO.Producto> productos = AppContext.getInstance().productoDAO().findAll();
            List<InventarioDAO.InventarioRow> inventario = AppContext.getInstance().inventarioDAO().findAll();

            Files.writeString(dir.resolve("ventas.csv"), TicketService.generarCsvVentas(ventas));
            Files.writeString(dir.resolve("productos.csv"), TicketService.generarCsvProductos(productos));
            Files.writeString(dir.resolve("inventario.csv"), TicketService.generarCsvInventario(inventario));

            lblStatus.setText("CSV exportado en " + dir.getFileName());
        } catch (Exception e) {
            log.error("Error exportando CSV", e);
            lblStatus.setText("Error al exportar CSV");
        }
    }

    @FXML
    private void handleCerrarTurno() {
        if (!items.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "Hay productos en el carrito. Finalice o cancele la venta antes de cerrar turno.",
                    ButtonType.OK);
            alert.showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Cerrar turno actual?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                AppContext.getInstance().turnoDAO().cerrar(turnoId);
                Stage stage = (Stage) lblTurno.getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 800, 600);
                scene.getStylesheets().add(getClass().getResource("/css/design-system.css").toExternalForm());
                stage.setScene(scene);
                stage.setMaximized(false);
                stage.centerOnScreen();
            } catch (Exception e) {
                log.error("Error cerrando turno", e);
            }
        }
    }
}
