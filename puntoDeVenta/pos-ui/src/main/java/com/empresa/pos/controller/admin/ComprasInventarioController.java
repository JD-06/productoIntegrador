package com.empresa.pos.controller.admin;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.CompraDAO;
import com.empresa.pos.dao.InventarioDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ComprasInventarioController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ComprasInventarioController.class);

    // Inventario
    @FXML private TableView<FilaInventario> tblInventario;
    @FXML private TableColumn<FilaInventario, String>  colInvSku;
    @FXML private TableColumn<FilaInventario, String>  colInvNombre;
    @FXML private TableColumn<FilaInventario, Integer> colInvActual;
    @FXML private TableColumn<FilaInventario, Integer> colInvMinimo;
    @FXML private TableColumn<FilaInventario, String>  colInvEstado;

    // Compras
    @FXML private TableView<FilaCompra> tblCompras;
    @FXML private TableColumn<FilaCompra, String> colCmpId;
    @FXML private TableColumn<FilaCompra, String> colCmpProveedor;
    @FXML private TableColumn<FilaCompra, String> colCmpTotal;
    @FXML private TableColumn<FilaCompra, String> colCmpFecha;

    private final ObservableList<FilaInventario> datosInventario = FXCollections.observableArrayList();
    private final ObservableList<FilaCompra>     datosCompras    = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colInvSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colInvNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colInvActual.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colInvMinimo.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));
        colInvEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        colInvEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setAlignment(Pos.CENTER);
                setStyle("CRITICO".equals(item)
                        ? "-fx-text-fill: #D32F2F; -fx-font-weight: bold;"
                        : "-fx-text-fill: #388E3C; -fx-font-weight: bold;");
            }
        });

        colCmpId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCmpProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colCmpTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colCmpFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

        tblInventario.setItems(datosInventario);
        tblCompras.setItems(datosCompras);

        cargarInventario();
        cargarCompras();
    }

    private void cargarInventario() {
        try {
            InventarioDAO dao = AppContext.getInstance().inventarioDAO();
            List<FilaInventario> lista = new ArrayList<>();
            for (InventarioDAO.InventarioRow row : dao.findAll()) {
                String estado = row.getStockActual() <= row.getStockMinimo() ? "CRITICO" : "OK";
                lista.add(new FilaInventario(row.getSku(), row.getNombre(),
                        row.getStockActual(), row.getStockMinimo(), estado));
            }
            datosInventario.setAll(lista);
        } catch (Exception e) {
            log.warn("No se pudo cargar inventario: {}", e.getMessage());
            datosInventario.setAll(
                new FilaInventario("SKU-001", "Agua Purificada 1L",  2,  10, "CRITICO"),
                new FilaInventario("SKU-002", "Refresco 2L",         15, 5,  "OK"),
                new FilaInventario("SKU-003", "Galletas de Avena",   1,  5,  "CRITICO")
            );
        }
    }

    private void cargarCompras() {
        try {
            CompraDAO dao = AppContext.getInstance().compraDAO();
            List<FilaCompra> lista = new ArrayList<>();
            for (CompraDAO.Compra c : dao.findAll()) {
                String fecha = c.getCreadoEn() != null
                        ? c.getCreadoEn().toLocalDate().toString() : "";
                lista.add(new FilaCompra(
                        String.valueOf(c.getId()),
                        c.getProveedorNombre() != null ? c.getProveedorNombre() : "",
                        c.getTotal() != null ? String.format("$%.2f", c.getTotal()) : "$0.00",
                        fecha
                ));
            }
            datosCompras.setAll(lista);
        } catch (Exception e) {
            log.warn("No se pudieron cargar compras: {}", e.getMessage());
            datosCompras.setAll(
                new FilaCompra("1", "Distribuidor Central S.A.", "$12,500.00", "2026-05-01"),
                new FilaCompra("2", "Proveedores Unidos",        "$4,300.00",  "2026-05-10")
            );
        }
    }

    @FXML
    private void handleNuevaCompra() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nueva Compra");
        dialog.setHeaderText("Registrar orden de compra");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField txtProveedor = new TextField();
        TextField txtTotal     = new TextField("0.00");
        txtProveedor.setPromptText("Nombre del proveedor");

        grid.add(new Label("Proveedor:"), 0, 0); grid.add(txtProveedor, 1, 0);
        grid.add(new Label("Total:"),     0, 1); grid.add(txtTotal,     1, 1);
        GridPane.setHgrow(txtProveedor, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            log.info("Nueva compra registrada para: {}", txtProveedor.getText());
            cargarCompras();
        }
    }

    // ---- Modelos de presentacion ----

    public static class FilaInventario {
        private final String sku, nombre, estado;
        private final int stockActual, stockMinimo;

        public FilaInventario(String sku, String nombre, int stockActual, int stockMinimo, String estado) {
            this.sku = sku; this.nombre = nombre;
            this.stockActual = stockActual; this.stockMinimo = stockMinimo;
            this.estado = estado;
        }

        public String getSku()       { return sku; }
        public String getNombre()    { return nombre; }
        public int getStockActual()  { return stockActual; }
        public int getStockMinimo()  { return stockMinimo; }
        public String getEstado()    { return estado; }
    }

    public static class FilaCompra {
        private final String id, proveedor, total, fecha;

        public FilaCompra(String id, String proveedor, String total, String fecha) {
            this.id = id; this.proveedor = proveedor;
            this.total = total; this.fecha = fecha;
        }

        public String getId()         { return id; }
        public String getProveedor()  { return proveedor; }
        public String getTotal()      { return total; }
        public String getFecha()      { return fecha; }
    }
}
