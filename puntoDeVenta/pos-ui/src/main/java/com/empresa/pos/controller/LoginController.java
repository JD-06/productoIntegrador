package com.empresa.pos.controller;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.UsuarioDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @FXML private ComboBox<String> cmbUsuario;
    @FXML private PasswordField txtPin;
    @FXML private TextField txtFondoInicial;
    @FXML private VBox vboxFondo;
    @FXML private Label lblError;

    private List<UsuarioDAO.UsuarioRow> usuarios;

    @FXML
    public void initialize() {
        cargarUsuarios();
        txtPin.setOnAction(event -> handleAbrirTurno());
        txtFondoInicial.setOnAction(event -> handleAbrirTurno());

        cmbUsuario.setOnAction(e -> {
            UsuarioDAO.UsuarioRow sel = getUsuarioSeleccionado();
            boolean esAdmin = sel != null && "ADMIN".equals(sel.getRol());
            vboxFondo.setVisible(!esAdmin);
            vboxFondo.setManaged(!esAdmin);
        });
    }

    private void cargarUsuarios() {
        try {
            usuarios = AppContext.getInstance().usuarioDAO().findAllActivos();
            for (UsuarioDAO.UsuarioRow u : usuarios) {
                cmbUsuario.getItems().add(u.getNombre());
            }
        } catch (Exception e) {
            log.warn("No se pudieron cargar usuarios desde BD: {}", e.getMessage());
            lblError.setText("Sin conexion a la base de datos.");
        }
    }

    private UsuarioDAO.UsuarioRow getUsuarioSeleccionado() {
        String nombre = cmbUsuario.getValue();
        if (nombre == null || usuarios == null) return null;
        return usuarios.stream()
                .filter(u -> u.getNombre().equals(nombre))
                .findFirst().orElse(null);
    }

    @FXML
    private void handleAbrirTurno() {
        String nombre = cmbUsuario.getValue();
        String pin    = txtPin.getText();
        String fondo  = txtFondoInicial.getText();

        UsuarioDAO.UsuarioRow usuario = getUsuarioSeleccionado();
        boolean esAdmin = usuario != null && "ADMIN".equals(usuario.getRol());

        if (nombre == null || pin.isEmpty() || (!esAdmin && fondo.isEmpty())) {
            lblError.setText("Por favor, complete todos los campos.");
            return;
        }

        try {
            String pinHash = sha256(pin);
            UsuarioDAO.UsuarioRow verificado = AppContext.getInstance()
                    .usuarioDAO().verificarPin(nombre, pinHash);

            if (verificado == null) {
                lblError.setText("PIN incorrecto. Intente de nuevo.");
                return;
            }

            Stage stage = (Stage) cmbUsuario.getScene().getWindow();
            if ("ADMIN".equals(verificado.getRol())) {
                navegarAlAdmin(stage, verificado.getNombre());
            } else {
                navegarAlPOS(stage, verificado);
            }

        } catch (Exception e) {
            log.error("Error al verificar login", e);
            lblError.setText("Error al verificar credenciales.");
        }
    }

    private void navegarAlAdmin(Stage stage, String nombre) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin_dashboard.fxml"));
        Parent root = loader.load();
        AdminController ctrl = loader.getController();
        ctrl.setNombreAdmin(nombre);
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/css/design-system.css").toExternalForm());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.centerOnScreen();
    }

    private void navegarAlPOS(Stage stage, UsuarioDAO.UsuarioRow cajero) throws IOException {
        String fondoStr = txtFondoInicial.getText().trim();
        BigDecimal fondo = fondoStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(fondoStr);
        int turnoId = AppContext.getInstance().turnoDAO().abrir(cajero.getId(), fondo);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_pos.fxml"));
        Parent root = loader.load();

        POSController ctrl = loader.getController();
        ctrl.setCajero(cajero.getId(), cajero.getNombre());
        ctrl.setTurno(turnoId);

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/css/design-system.css").toExternalForm());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.centerOnScreen();
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
