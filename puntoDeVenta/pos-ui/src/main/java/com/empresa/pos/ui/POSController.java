package com.empresa.pos.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;

public class POSController implements Initializable {

    @FXML private TextField txtBusqueda;
    @FXML private TableView<?> tblCarrito;
    @FXML private Label lblTotal;
    @FXML private Label lblSubtotal;
    @FXML private Label lblIva;
    @FXML private Label lblUsuario;
    @FXML private Label lblTurno;
    @FXML private Label lblStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inicialización de la lógica del controlador
        setupTable();
        setupKeyboardShortcuts();
    }

    private void setupTable() {
        // Configurar columnas de la tabla (asociar con propiedades del modelo)
    }

    private void setupKeyboardShortcuts() {
        // Lógica para manejar F5, F12, etc.
    }

    @FXML
    private void handleCobrar() {
        // Abrir modal de cobro
    }
}
