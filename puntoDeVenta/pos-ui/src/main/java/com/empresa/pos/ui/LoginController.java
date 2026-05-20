package com.empresa.pos.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML private ComboBox<String> cmbUsuario;
    @FXML private PasswordField txtPin;
    @FXML private TextField txtFondoInicial;
    @FXML private Label lblError;

    @FXML
    public void initialize() {
        // Simulación de carga de usuarios (esto vendría de pos-data)
        cmbUsuario.getItems().addAll("Admin", "Cajero 1", "Cajero 2");
        
        // Atajo Enter para el botón de abrir turno
        txtPin.setOnAction(event -> handleAbrirTurno());
        txtFondoInicial.setOnAction(event -> handleAbrirTurno());
    }

    @FXML
    private void handleAbrirTurno() {
        String usuario = cmbUsuario.getValue();
        String pin = txtPin.getText();
        String fondo = txtFondoInicial.getText();

        if (usuario == null || pin.isEmpty() || fondo.isEmpty()) {
            lblError.setText("Por favor, complete todos los campos.");
            return;
        }

        // Simulación de validación de PIN (bcrypt se implementará luego en pos-core)
        if ("1234".equals(pin)) {
            navigateToMain();
        } else {
            lblError.setText("PIN incorrecto. Intente de nuevo.");
        }
    }

    private void navigateToMain() {
        try {
            Stage stage = (Stage) cmbUsuario.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_pos.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(getClass().getResource("/css/design-system.css").toExternalForm());
            
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            lblError.setText("Error al cargar la pantalla principal.");
            e.printStackTrace();
        }
    }
}
