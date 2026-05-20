package com.empresa.pos.controller.admin;

import com.empresa.pos.controller.LoginController;
import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.UsuarioDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class UsuariosController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(UsuariosController.class);

    @FXML private TableView<UsuarioDAO.UsuarioRow> tblUsuarios;
    @FXML private TableColumn<UsuarioDAO.UsuarioRow, Integer> colId;
    @FXML private TableColumn<UsuarioDAO.UsuarioRow, String>  colNombre;
    @FXML private TableColumn<UsuarioDAO.UsuarioRow, String>  colRol;
    @FXML private TableColumn<UsuarioDAO.UsuarioRow, Void>    colActivo;
    @FXML private TableColumn<UsuarioDAO.UsuarioRow, Void>    colAccion;

    private final ObservableList<UsuarioDAO.UsuarioRow> datos = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));

        colActivo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                UsuarioDAO.UsuarioRow u = getTableRow().getItem();
                Label badge = new Label(u.isActivo() ? "ACTIVO" : "INACTIVO");
                badge.setStyle(u.isActivo()
                    ? "-fx-background-color:#2E7D32;-fx-text-fill:white;-fx-padding:3 8;-fx-background-radius:4;"
                    : "-fx-background-color:#757575;-fx-text-fill:white;-fx-padding:3 8;-fx-background-radius:4;");
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });

        colAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btnToggle  = new Button();
            private final Button btnPin     = new Button("Cambiar PIN");
            {
                btnPin.setStyle("-fx-background-color:#1A2D5A;-fx-text-fill:white;-fx-padding:4 10;");
                btnToggle.setOnAction(e -> {
                    UsuarioDAO.UsuarioRow u = getTableRow().getItem();
                    if (u == null) return;
                    try {
                        AppContext.getInstance().usuarioDAO().toggleActivo(u.getId(), !u.isActivo());
                        cargarDatos();
                    } catch (Exception ex) {
                        log.error("Error al cambiar estado: {}", ex.getMessage());
                    }
                });
                btnPin.setOnAction(e -> {
                    UsuarioDAO.UsuarioRow u = getTableRow().getItem();
                    if (u == null) return;
                    mostrarDialogCambiarPin(u);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                UsuarioDAO.UsuarioRow u = getTableRow().getItem();
                boolean esAdminRow = "ADMIN".equals(u.getRol());
                btnToggle.setText(u.isActivo() ? "Desactivar" : "Activar");
                btnToggle.setStyle(u.isActivo()
                    ? "-fx-background-color:#C62828;-fx-text-fill:white;-fx-padding:4 10;"
                    : "-fx-background-color:#2E7D32;-fx-text-fill:white;-fx-padding:4 10;");
                btnToggle.setDisable(esAdminRow);
                HBox box = new HBox(6, btnToggle, btnPin);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        tblUsuarios.setItems(datos);
        cargarDatos();
    }

    private void cargarDatos() {
        try {
            List<UsuarioDAO.UsuarioRow> lista = AppContext.getInstance().usuarioDAO().findAll();
            datos.setAll(lista);
        } catch (Exception e) {
            log.warn("No se pudieron cargar usuarios: {}", e.getMessage());
        }
    }

    @FXML
    private void handleNuevoUsuario() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Usuario");
        dialog.setHeaderText("Crear usuario del sistema");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(10));

        TextField txtNombre = new TextField();
        PasswordField txtPin = new PasswordField();
        PasswordField txtPin2 = new PasswordField();
        ComboBox<UsuarioDAO.RolRow> cmbRol = new ComboBox<>();

        txtNombre.setPromptText("Nombre del operador");
        txtPin.setPromptText("PIN de acceso");
        txtPin2.setPromptText("Confirmar PIN");

        try {
            cmbRol.setItems(FXCollections.observableArrayList(
                    AppContext.getInstance().usuarioDAO().findRoles()));
            if (!cmbRol.getItems().isEmpty()) cmbRol.setValue(cmbRol.getItems().get(1));
        } catch (Exception e) {
            log.warn("No se pudieron cargar roles: {}", e.getMessage());
        }

        grid.add(new Label("Nombre:"), 0, 0); grid.add(txtNombre, 1, 0);
        grid.add(new Label("PIN:"),    0, 1); grid.add(txtPin,    1, 1);
        grid.add(new Label("Confirmar PIN:"), 0, 2); grid.add(txtPin2, 1, 2);
        grid.add(new Label("Rol:"),    0, 3); grid.add(cmbRol,   1, 3);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String nombre = txtNombre.getText().trim();
            String pin    = txtPin.getText();
            String pin2   = txtPin2.getText();

            if (nombre.isEmpty() || pin.isEmpty()) {
                mostrarError("Nombre y PIN son obligatorios.");
                return;
            }
            if (!pin.equals(pin2)) {
                mostrarError("Los PINs no coinciden.");
                return;
            }
            if (cmbRol.getValue() == null) {
                mostrarError("Selecciona un rol.");
                return;
            }

            try {
                String pinHash = LoginController.sha256(pin);
                AppContext.getInstance().usuarioDAO()
                        .insert(nombre, pinHash, cmbRol.getValue().getId());
                cargarDatos();
            } catch (Exception e) {
                log.error("Error al crear usuario: {}", e.getMessage());
                mostrarError("No se pudo crear el usuario: " + e.getMessage());
            }
        }
    }

    private void mostrarDialogCambiarPin(UsuarioDAO.UsuarioRow usuario) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Cambiar PIN");
        dialog.setHeaderText("Cambiar PIN de: " + usuario.getNombre());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(10));

        PasswordField txtNuevo  = new PasswordField();
        PasswordField txtNuevo2 = new PasswordField();
        txtNuevo.setPromptText("Nuevo PIN");
        txtNuevo2.setPromptText("Confirmar nuevo PIN");

        grid.add(new Label("Nuevo PIN:"),    0, 0); grid.add(txtNuevo,  1, 0);
        grid.add(new Label("Confirmar:"),    0, 1); grid.add(txtNuevo2, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String pin  = txtNuevo.getText();
            String pin2 = txtNuevo2.getText();
            if (pin.isEmpty() || !pin.equals(pin2)) {
                mostrarError("Los PINs no coinciden o están vacíos.");
                return;
            }
            try {
                AppContext.getInstance().usuarioDAO()
                        .cambiarPin(usuario.getId(), LoginController.sha256(pin));
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setContentText("PIN actualizado correctamente.");
                ok.showAndWait();
            } catch (Exception e) {
                mostrarError("No se pudo cambiar el PIN: " + e.getMessage());
            }
        }
    }

    private void mostrarError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
