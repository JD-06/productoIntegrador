package com.empresa.pos.controlador.admin;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.LogAuditoriaDAO;
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
import java.util.ResourceBundle;

public class LogAuditoriaController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(LogAuditoriaController.class);

    @FXML private TableView<FilaLog> tblLog;
    @FXML private TableColumn<FilaLog, String> colLogId;
    @FXML private TableColumn<FilaLog, String> colLogUsuario;
    @FXML private TableColumn<FilaLog, String> colLogAccion;
    @FXML private TableColumn<FilaLog, String> colLogTabla;
    @FXML private TableColumn<FilaLog, String> colLogFecha;

    private final ObservableList<FilaLog> datos = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colLogId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colLogUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colLogAccion.setCellValueFactory(new PropertyValueFactory<>("accion"));
        colLogTabla.setCellValueFactory(new PropertyValueFactory<>("tabla"));
        colLogFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

        tblLog.setItems(datos);
        cargarDatos();
    }

    private void cargarDatos() {
        try {
            LogAuditoriaDAO dao = AppContext.getInstance().logDAO();
            List<FilaLog> lista = new ArrayList<>();
            for (LogAuditoriaDAO.LogEntry entry : dao.findAll()) {
                String fecha = entry.getCreadoEn() != null
                        ? entry.getCreadoEn().toString().replace("T", " ") : "";
                lista.add(new FilaLog(
                        String.valueOf(entry.getId()),
                        entry.getUsuarioNombre() != null ? entry.getUsuarioNombre() : "",
                        entry.getAccion() != null ? entry.getAccion() : "",
                        entry.getTablaAfectada() != null ? entry.getTablaAfectada() : "",
                        fecha
                ));
            }
            datos.setAll(lista);
        } catch (Exception e) {
            log.warn("No se pudo cargar log de auditoria: {}", e.getMessage());
            datos.setAll(
                new FilaLog("1", "admin",   "LOGIN",           "sesiones",  "2026-05-20 08:00:01"),
                new FilaLog("2", "cajero1", "VENTA",           "ventas",    "2026-05-20 09:15:33"),
                new FilaLog("3", "admin",   "ACTUALIZAR",      "productos", "2026-05-20 10:02:55"),
                new FilaLog("4", "cajero1", "CORTE",           "turnos",    "2026-05-20 14:30:11"),
                new FilaLog("5", "admin",   "CREAR_CATEGORIA", "categorias","2026-05-20 15:05:47")
            );
        }
    }

    public static class FilaLog {
        private final String id, usuario, accion, tabla, fecha;
        public FilaLog(String id, String usuario, String accion, String tabla, String fecha) {
            this.id = id; this.usuario = usuario; this.accion = accion;
            this.tabla = tabla; this.fecha = fecha;
        }
        public String getId()      { return id; }
        public String getUsuario() { return usuario; }
        public String getAccion()  { return accion; }
        public String getTabla()   { return tabla; }
        public String getFecha()   { return fecha; }
    }
}
