package com.empresa.pos.controlador.admin;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.CategoriaDAO;
import com.empresa.pos.dao.InventarioDAO;
import com.empresa.pos.dao.JSONImportService;
import com.empresa.pos.dao.ProductoDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CatalogoController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CatalogoController.class);

    @FXML private TextField txtBusqueda;
    @FXML private ComboBox<CategoriaItem> cmbCategoria;
    @FXML private TableView<ProductoDAO.Producto> tblProductos;
    @FXML private TableColumn<ProductoDAO.Producto, Void>       colImagen;
    @FXML private TableColumn<ProductoDAO.Producto, String>     colSku;
    @FXML private TableColumn<ProductoDAO.Producto, String>     colNombre;
    @FXML private TableColumn<ProductoDAO.Producto, String>     colMarca;
    @FXML private TableColumn<ProductoDAO.Producto, String>     colCategoria;
    @FXML private TableColumn<ProductoDAO.Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<ProductoDAO.Producto, Integer>    colStock;
    @FXML private TableColumn<ProductoDAO.Producto, String>     colUnidad;
    @FXML private TableColumn<ProductoDAO.Producto, Boolean>    colActivo;

    private final ObservableList<ProductoDAO.Producto> todos = FXCollections.observableArrayList();
    private FilteredList<ProductoDAO.Producto> filtrados;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarColumnas();
        filtrados = new FilteredList<>(todos, p -> true);
        tblProductos.setItems(filtrados);

        txtBusqueda.textProperty().addListener((obs, o, n) -> aplicarFiltro());
        cmbCategoria.valueProperty().addListener((obs, o, n) -> aplicarFiltro());

        cargarCategorias();
        cargarProductos();
    }

    private void configurarColumnas() {
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colUnidad.setCellValueFactory(new PropertyValueFactory<>("unidad"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        colImagen.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                ProductoDAO.Producto p = getTableRow().getItem();
                setGraphic(crearImagenCell(p.getImagenLocal()));
                setAlignment(Pos.CENTER);
            }
        });

        colPrecio.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });

        colActivo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item ? "Si" : "No"));
            }
        });
    }

    private javafx.scene.Node crearImagenCell(String imagenLocal) {
        if (imagenLocal != null && !imagenLocal.isBlank()) {
            try {
                String imagesPath = AppContext.getInstance().getImagesPath();
                Path imgPath = Path.of(imagesPath, imagenLocal);
                if (Files.exists(imgPath)) {
                    String uri = imgPath.toUri().toString();
                    ImageView iv = new ImageView(new Image(uri, 48, 48, true, true));
                    iv.setFitWidth(48);
                    iv.setFitHeight(48);
                    return iv;
                }
            } catch (Exception e) {
                log.debug("No se pudo cargar imagen: {}", imagenLocal);
            }
        }
        Rectangle placeholder = new Rectangle(40, 40);
        placeholder.setFill(Color.LIGHTGRAY);
        placeholder.setArcWidth(6);
        placeholder.setArcHeight(6);
        return placeholder;
    }

    private void cargarCategorias() {
        List<CategoriaItem> items = new ArrayList<>();
        items.add(new CategoriaItem(0, "Todas las categorias"));
        try {
            for (CategoriaDAO.Categoria cat : AppContext.getInstance().categoriaDAO().findAll()) {
                items.add(new CategoriaItem(cat.getId(), cat.getNombre()));
            }
        } catch (Exception e) {
            log.warn("No se pudieron cargar categorias para filtro: {}", e.getMessage());
        }
        cmbCategoria.setItems(FXCollections.observableArrayList(items));
        cmbCategoria.setValue(items.get(0));
    }

    private void cargarProductos() {
        try {
            todos.setAll(AppContext.getInstance().productoDAO().findAll());
        } catch (Exception e) {
            log.warn("No se pudieron cargar productos: {}", e.getMessage());
            todos.clear();
        }
    }

    private void aplicarFiltro() {
        String texto = txtBusqueda.getText().trim().toLowerCase();
        CategoriaItem catSel = cmbCategoria.getValue();
        int catId = (catSel == null) ? 0 : catSel.id;

        filtrados.setPredicate(p -> {
            boolean coincideTexto = texto.isEmpty()
                    || (p.getNombre() != null && p.getNombre().toLowerCase().contains(texto))
                    || (p.getSku() != null && p.getSku().toLowerCase().contains(texto))
                    || (p.getMarca() != null && p.getMarca().toLowerCase().contains(texto));
            boolean coincideCat = catId == 0 || p.getCategoriaId() == catId;
            return coincideTexto && coincideCat;
        });
    }

    @FXML
    private void handleImportarJSON() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo JSON");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = fc.showOpenDialog(tblProductos.getScene().getWindow());
        if (file == null) return;

        try {
            AppContext ctx = AppContext.getInstance();
            JSONImportService importService = new JSONImportService(ctx.categoriaDAO(), ctx.getDataSource());
            JSONImportService.ImportResult result = importService.importar(file.getAbsolutePath());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Importacion completada");
            alert.setHeaderText("Resultado de la importacion JSON");
            alert.setContentText("Insertados: " + result.getInsertados()
                    + "\nActualizados: " + result.getActualizados()
                    + "\nErrores: " + result.getErrores());
            alert.showAndWait();
            cargarProductos();
        } catch (Exception e) {
            log.error("Error al importar JSON: {}", e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de importacion");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo importar el archivo: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleNuevoProducto() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Producto");
        dialog.setHeaderText("Registrar nuevo producto");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField txtSku    = new TextField();
        TextField txtNombre = new TextField();
        TextField txtMarca  = new TextField();
        TextField txtPrecio = new TextField("0.00");
        TextField txtUnidad = new TextField("PZA");

        txtSku.setPromptText("Ej: SKU-001");
        txtNombre.setPromptText("Nombre del producto");
        txtMarca.setPromptText("Marca");

        grid.add(new Label("SKU:"),    0, 0); grid.add(txtSku,    1, 0);
        grid.add(new Label("Nombre:"), 0, 1); grid.add(txtNombre, 1, 1);
        grid.add(new Label("Marca:"),  0, 2); grid.add(txtMarca,  1, 2);
        grid.add(new Label("Precio:"), 0, 3); grid.add(txtPrecio, 1, 3);
        grid.add(new Label("Unidad:"), 0, 4); grid.add(txtUnidad, 1, 4);

        GridPane.setHgrow(txtNombre, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (txtNombre.getText().trim().isEmpty() || txtSku.getText().trim().isEmpty()) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setContentText("SKU y Nombre son obligatorios.");
                err.showAndWait();
                return;
            }
            try {
                ProductoDAO.Producto p = new ProductoDAO.Producto();
                p.setSku(txtSku.getText().trim());
                p.setNombre(txtNombre.getText().trim());
                p.setMarca(txtMarca.getText().trim());
                p.setUnidad(txtUnidad.getText().trim());
                p.setPrecio(new BigDecimal(txtPrecio.getText().trim().replace(",", ".")));
                p.setActivo(true);
                AppContext.getInstance().productoDAO().insert(p);
                cargarProductos();
            } catch (Exception e) {
                log.error("Error al guardar producto: {}", e.getMessage());
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setContentText("No se pudo guardar: " + e.getMessage());
                err.showAndWait();
            }
        }
    }

    @FXML
    private void handleAgregarStock() {
        ProductoDAO.Producto producto = tblProductos.getSelectionModel().getSelectedItem();
        if (producto == null) {
            mostrarMensaje(Alert.AlertType.WARNING, "Selecciona un producto", "Primero selecciona un producto del catalogo.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Agregar Stock");
        dialog.setHeaderText("Actualizar stock de: " + producto.getNombre());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField txtActual = new TextField(String.valueOf(producto.getStockActual()));
        TextField txtMinimo = new TextField(String.valueOf(producto.getStockMinimo()));

        grid.add(new Label("Stock actual:"), 0, 0);
        grid.add(txtActual, 1, 0);
        grid.add(new Label("Stock minimo:"), 0, 1);
        grid.add(txtMinimo, 1, 1);
        GridPane.setHgrow(txtActual, Priority.ALWAYS);
        GridPane.setHgrow(txtMinimo, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int stockActual = Integer.parseInt(txtActual.getText().trim());
                int stockMinimo = Integer.parseInt(txtMinimo.getText().trim());
                if (stockActual < 0 || stockMinimo < 0) {
                    throw new IllegalArgumentException("Los valores no pueden ser negativos.");
                }

                AppContext.getInstance().inventarioDAO()
                        .establecerStock(producto.getId(), stockActual, stockMinimo);
                cargarProductos();
                mostrarMensaje(Alert.AlertType.INFORMATION, "Stock actualizado",
                        "Se actualizo el inventario de " + producto.getNombre() + ".");
            } catch (Exception e) {
                log.error("Error al actualizar stock", e);
                mostrarMensaje(Alert.AlertType.ERROR, "Error al actualizar stock", e.getMessage());
            }
        }
    }

    private void mostrarMensaje(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // Helper inner class for ComboBox display
    private static class CategoriaItem {
        final int id;
        final String nombre;

        CategoriaItem(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() { return nombre; }
    }
}
