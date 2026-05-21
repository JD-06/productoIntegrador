package com.empresa.pos.controlador.admin;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.UsuarioDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class SeguridadController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SeguridadController.class);

    private static final Map<String, List<String>> PERMISOS_POR_ROL = Map.of(
        "ADMIN",      List.of("Ver Monitor", "Ver Catalogos", "Ver CRM", "Ver ERP", "Ver Seguridad",
                              "Ver Log", "Modificar Productos", "Modificar Usuarios", "Cerrar Turno"),
        "SUPERVISOR", List.of("Ver Monitor", "Ver Catalogos", "Ver CRM", "Modificar Productos",
                              "Cerrar Turno"),
        "CAJERO",     List.of("Ver Monitor", "Realizar Venta", "Abrir Turno", "Cerrar Turno")
    );

    @FXML private TableView<FilaUsuario> tblUsuarios;
    @FXML private TableColumn<FilaUsuario, String>  colUsrNombre;
    @FXML private TableColumn<FilaUsuario, String>  colUsrRol;
    @FXML private TableColumn<FilaUsuario, String>  colUsrActivo;

    @FXML private ListView<String> listPermisos;
    @FXML private Label lblRolSeleccionado;

    private final ObservableList<FilaUsuario> datosUsuarios = FXCollections.observableArrayList();
    private final ObservableList<String>      datosPermisos = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colUsrNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colUsrRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colUsrActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        tblUsuarios.setItems(datosUsuarios);
        listPermisos.setItems(datosPermisos);

        tblUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) mostrarPermisosDeRol(n.getRol());
        });

        cargarUsuarios();
    }

    private void cargarUsuarios() {
        try {
            UsuarioDAO dao = AppContext.getInstance().usuarioDAO();
            List<FilaUsuario> lista = new ArrayList<>();
            for (UsuarioDAO.UsuarioRow u : dao.findAll()) {
                lista.add(new FilaUsuario(
                        u.getNombre() != null ? u.getNombre() : "",
                        u.getRol() != null ? u.getRol() : "",
                        u.isActivo() ? "Si" : "No"
                ));
            }
            datosUsuarios.setAll(lista);
        } catch (Exception e) {
            log.warn("No se pudieron cargar usuarios: {}", e.getMessage());
            datosUsuarios.setAll(
                new FilaUsuario("admin",      "ADMIN",      "Si"),
                new FilaUsuario("cajero1",    "CAJERO",     "Si"),
                new FilaUsuario("cajero2",    "CAJERO",     "Si"),
                new FilaUsuario("supervisor", "SUPERVISOR", "Si")
            );
        }
    }

    private void mostrarPermisosDeRol(String rol) {
        datosPermisos.clear();
        lblRolSeleccionado.setText("Permisos del rol: " + rol);
        List<String> permisos = PERMISOS_POR_ROL.getOrDefault(rol, List.of("Sin permisos definidos para: " + rol));
        datosPermisos.setAll(permisos);
    }

    public static class FilaUsuario {
        private final String nombre, rol, activo;
        public FilaUsuario(String nombre, String rol, String activo) {
            this.nombre = nombre; this.rol = rol; this.activo = activo;
        }
        public String getNombre() { return nombre; }
        public String getRol()    { return rol; }
        public String getActivo() { return activo; }
    }
}
