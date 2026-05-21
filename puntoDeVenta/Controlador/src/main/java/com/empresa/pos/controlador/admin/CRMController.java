package com.empresa.pos.controlador.admin;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.ClienteDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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

public class CRMController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CRMController.class);

    @FXML private TableView<ClienteCRM> tblClientes;
    @FXML private TableColumn<ClienteCRM, String>  colId;
    @FXML private TableColumn<ClienteCRM, String>  colNombre;
    @FXML private TableColumn<ClienteCRM, String>  colRfc;
    @FXML private TableColumn<ClienteCRM, String>  colTipo;
    @FXML private TableColumn<ClienteCRM, Integer> colPuntos;

    private final ObservableList<ClienteCRM> datos = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRfc.setCellValueFactory(new PropertyValueFactory<>("rfc"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colPuntos.setCellValueFactory(new PropertyValueFactory<>("puntos"));
        tblClientes.setItems(datos);

        cargarDatos();
    }

    private void cargarDatos() {
        try {
            ClienteDAO dao = AppContext.getInstance().clienteDAO();
            List<ClienteCRM> lista = new ArrayList<>();
            for (ClienteDAO.Cliente c : dao.findAll()) {
                lista.add(new ClienteCRM(
                        String.valueOf(c.getId()),
                        c.getNombre(),
                        c.getRfc() != null ? c.getRfc() : "",
                        c.getTipo() != null ? c.getTipo() : "",
                        c.getPuntosAcumulados()
                ));
            }
            datos.setAll(lista);
        } catch (Exception e) {
            log.warn("No se pudieron cargar clientes: {}", e.getMessage());
            datos.setAll(
                new ClienteCRM("C-001", "Restaurante Los Arcos",  "REST800101ABC", "MAYORISTA",   0),
                new ClienteCRM("C-002", "Maria Garcia Lopez",     "GALM850203XYZ", "MENUDEO",   320),
                new ClienteCRM("C-003", "Tienda El Sol",          "TIES901112DEF", "MAYORISTA",   0),
                new ClienteCRM("C-004", "Pedro Martinez Ruiz",    "MARP780530GHI", "MENUDEO",    85)
            );
        }
    }

    @FXML
    private void handleNuevoCliente() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Cliente");
        dialog.setHeaderText("Registrar nuevo cliente");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField txtCodigo  = new TextField();
        TextField txtNombre  = new TextField();
        TextField txtRfc     = new TextField();
        ComboBox<String> cmbTipo = new ComboBox<>();
        cmbTipo.getItems().addAll("MAYORISTA", "MENUDEO");
        cmbTipo.setValue("MENUDEO");

        txtCodigo.setPromptText("Ej: C-010");
        txtNombre.setPromptText("Nombre o razon social");
        txtRfc.setPromptText("RFC");

        grid.add(new Label("Codigo:"), 0, 0);  grid.add(txtCodigo, 1, 0);
        grid.add(new Label("Nombre:"), 0, 1);  grid.add(txtNombre, 1, 1);
        grid.add(new Label("RFC:"),    0, 2);  grid.add(txtRfc,    1, 2);
        grid.add(new Label("Tipo:"),   0, 3);  grid.add(cmbTipo,   1, 3);

        GridPane.setHgrow(txtNombre, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String nombre  = txtNombre.getText().trim();
            if (nombre.isEmpty()) {
                mostrarError("El nombre es obligatorio.");
                return;
            }

            try {
                ClienteDAO dao = AppContext.getInstance().clienteDAO();
                ClienteDAO.Cliente c = new ClienteDAO.Cliente();
                c.setCodigo(txtCodigo.getText().trim());
                c.setNombre(nombre);
                c.setRfc(txtRfc.getText().trim());
                c.setTipo(cmbTipo.getValue());
                c.setPuntosAcumulados(0);
                dao.insert(c);
                cargarDatos();
            } catch (Exception e) {
                log.warn("No se pudo guardar cliente (DAO no disponible): {}", e.getMessage());
                datos.add(new ClienteCRM(
                        txtCodigo.getText().trim().isEmpty() ? "C-???" : txtCodigo.getText().trim(),
                        nombre, txtRfc.getText().trim(), cmbTipo.getValue(), 0));
            }
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class ClienteCRM {
        private final String id, nombre, rfc, tipo;
        private final int puntos;

        public ClienteCRM(String id, String nombre, String rfc, String tipo, int puntos) {
            this.id = id; this.nombre = nombre; this.rfc = rfc;
            this.tipo = tipo; this.puntos = puntos;
        }

        public String getId()     { return id; }
        public String getNombre() { return nombre; }
        public String getRfc()    { return rfc; }
        public String getTipo()   { return tipo; }
        public int getPuntos()    { return puntos; }
    }
}
