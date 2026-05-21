package com.empresa.pos.controlador;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    @FXML private Label lblAdminNombre;
    @FXML private StackPane contenido;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showMonitorCortes();
    }

    public void setNombreAdmin(String nombre) {
        lblAdminNombre.setText("Admin: " + nombre);
    }

    @FXML private void showMonitorCortes()      { cargarVista("/fxml/admin/monitor_cortes.fxml"); }
    @FXML private void showCategorias()         { cargarVista("/fxml/admin/categorias.fxml"); }
    @FXML private void showCRM()                { cargarVista("/fxml/admin/directorio_crm.fxml"); }
    @FXML private void showProximamente()       { cargarVista("/fxml/admin/proximamente.fxml"); }
    @FXML private void showCatalogo()           { cargarVista("/fxml/admin/catalogo_productos.fxml"); }
    @FXML private void showComprasInventario()  { cargarVista("/fxml/admin/compras_inventario.fxml"); }
    @FXML private void showCxC()                { cargarVista("/fxml/admin/cxc.fxml"); }
    @FXML private void showCxP()                { cargarVista("/fxml/admin/cxp.fxml"); }
    @FXML private void showNomina()             { cargarVista("/fxml/admin/nomina.fxml"); }
    @FXML private void showSeguridad()          { cargarVista("/fxml/admin/seguridad.fxml"); }
    @FXML private void showLog()                { cargarVista("/fxml/admin/log_auditoria.fxml"); }
    @FXML private void showUtilerias()          { cargarVista("/fxml/admin/utilerias.fxml"); }
    @FXML private void showPremios()            { cargarVista("/fxml/admin/premios_lealtad.fxml"); }
    @FXML private void showUsuarios()           { cargarVista("/fxml/admin/usuarios.fxml"); }

    private void cargarVista(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node vista = loader.load();
            contenido.getChildren().setAll(vista);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSalir() {
        try {
            Stage stage = (Stage) contenido.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/css/design-system.css").toExternalForm());
            stage.setMaximized(false);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
