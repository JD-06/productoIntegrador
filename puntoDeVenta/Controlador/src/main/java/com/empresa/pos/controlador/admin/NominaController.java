package com.empresa.pos.controlador.admin;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.EmpleadoDAO;
import com.empresa.pos.dao.NominaDAO;
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

public class NominaController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(NominaController.class);

    @FXML private ComboBox<PeriodoItem> cmbPeriodos;

    @FXML private TableView<FilaEmpleado> tblEmpleados;
    @FXML private TableColumn<FilaEmpleado, String> colEmpNombre;
    @FXML private TableColumn<FilaEmpleado, String> colEmpPuesto;
    @FXML private TableColumn<FilaEmpleado, String> colEmpSalario;
    @FXML private TableColumn<FilaEmpleado, String> colEmpActivo;

    @FXML private TableView<FilaNomina> tblNomina;
    @FXML private TableColumn<FilaNomina, String> colNomEmpleado;
    @FXML private TableColumn<FilaNomina, String> colNomSalario;
    @FXML private TableColumn<FilaNomina, String> colNomDeducciones;
    @FXML private TableColumn<FilaNomina, String> colNomNeto;

    private final ObservableList<FilaEmpleado> datosEmpleados = FXCollections.observableArrayList();
    private final ObservableList<FilaNomina>   datosNomina    = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colEmpNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEmpPuesto.setCellValueFactory(new PropertyValueFactory<>("puesto"));
        colEmpSalario.setCellValueFactory(new PropertyValueFactory<>("salario"));
        colEmpActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        colNomEmpleado.setCellValueFactory(new PropertyValueFactory<>("empleado"));
        colNomSalario.setCellValueFactory(new PropertyValueFactory<>("salarioBase"));
        colNomDeducciones.setCellValueFactory(new PropertyValueFactory<>("deducciones"));
        colNomNeto.setCellValueFactory(new PropertyValueFactory<>("neto"));

        tblEmpleados.setItems(datosEmpleados);
        tblNomina.setItems(datosNomina);

        cmbPeriodos.valueProperty().addListener((obs, o, n) -> cargarLineasNomina(n));

        cargarEmpleados();
        cargarPeriodos();
    }

    private void cargarEmpleados() {
        try {
            EmpleadoDAO dao = AppContext.getInstance().empleadoDAO();
            List<FilaEmpleado> lista = new ArrayList<>();
            for (EmpleadoDAO.Empleado emp : dao.findAll()) {
                lista.add(new FilaEmpleado(
                        emp.getNombreUsuario() != null ? emp.getNombreUsuario() : "",
                        emp.getPuesto() != null ? emp.getPuesto() : "",
                        emp.getSalario() != null ? String.format("$%.2f", emp.getSalario()) : "$0.00",
                        emp.isActivo() ? "Si" : "No"
                ));
            }
            datosEmpleados.setAll(lista);
        } catch (Exception e) {
            log.warn("No se pudieron cargar empleados: {}", e.getMessage());
            datosEmpleados.setAll(
                new FilaEmpleado("Juan Perez",    "Cajero",     "$8,500.00",  "Si"),
                new FilaEmpleado("Maria Lopez",   "Supervisor", "$12,000.00", "Si"),
                new FilaEmpleado("Carlos Ruiz",   "Almacenista","$7,200.00",  "Si")
            );
        }
    }

    private void cargarPeriodos() {
        List<PeriodoItem> periodos = new ArrayList<>();
        try {
            NominaDAO dao = AppContext.getInstance().nominaDAO();
            for (NominaDAO.PeriodoNomina p : dao.findPeriodos()) {
                periodos.add(new PeriodoItem(p.getId(), p.getPeriodo()));
            }
        } catch (Exception e) {
            log.warn("No se pudieron cargar periodos: {}", e.getMessage());
            periodos.add(new PeriodoItem(1, "Mayo 2026 - Quincenal 1"));
            periodos.add(new PeriodoItem(2, "Mayo 2026 - Quincenal 2"));
        }
        cmbPeriodos.setItems(FXCollections.observableArrayList(periodos));
        if (!periodos.isEmpty()) cmbPeriodos.setValue(periodos.get(0));
    }

    private void cargarLineasNomina(PeriodoItem periodo) {
        if (periodo == null) { datosNomina.clear(); return; }
        try {
            NominaDAO dao = AppContext.getInstance().nominaDAO();
            List<FilaNomina> lista = new ArrayList<>();
            for (NominaDAO.LineaNomina ln : dao.findByPeriodo(periodo.id)) {
                lista.add(new FilaNomina(
                        ln.getEmpleadoNombre() != null ? ln.getEmpleadoNombre() : "",
                        ln.getSalarioBase() != null ? String.format("$%.2f", ln.getSalarioBase()) : "$0.00",
                        ln.getDeducciones() != null ? String.format("$%.2f", ln.getDeducciones()) : "$0.00",
                        ln.getNetoPagar() != null ? String.format("$%.2f", ln.getNetoPagar()) : "$0.00"
                ));
            }
            datosNomina.setAll(lista);
        } catch (Exception e) {
            log.warn("No se pudieron cargar lineas de nomina: {}", e.getMessage());
            datosNomina.setAll(
                new FilaNomina("Juan Perez",  "$8,500.00",  "$1,020.00", "$7,480.00"),
                new FilaNomina("Maria Lopez", "$12,000.00", "$1,440.00", "$10,560.00"),
                new FilaNomina("Carlos Ruiz", "$7,200.00",  "$864.00",   "$6,336.00")
            );
        }
    }

    @FXML
    private void handleNuevoPeriodo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Nuevo Periodo");
        alert.setHeaderText(null);
        alert.setContentText("Creacion de nuevo periodo de nomina en desarrollo.");
        alert.showAndWait();
    }

    // ---- Items helper ----

    public static class PeriodoItem {
        final int id;
        final String descripcion;
        PeriodoItem(int id, String descripcion) { this.id = id; this.descripcion = descripcion; }
        @Override public String toString() { return descripcion; }
    }

    // ---- Modelos de presentacion ----

    public static class FilaEmpleado {
        private final String nombre, puesto, salario, activo;
        public FilaEmpleado(String nombre, String puesto, String salario, String activo) {
            this.nombre = nombre; this.puesto = puesto;
            this.salario = salario; this.activo = activo;
        }
        public String getNombre()  { return nombre; }
        public String getPuesto()  { return puesto; }
        public String getSalario() { return salario; }
        public String getActivo()  { return activo; }
    }

    public static class FilaNomina {
        private final String empleado, salarioBase, deducciones, neto;
        public FilaNomina(String empleado, String salarioBase, String deducciones, String neto) {
            this.empleado = empleado; this.salarioBase = salarioBase;
            this.deducciones = deducciones; this.neto = neto;
        }
        public String getEmpleado()    { return empleado; }
        public String getSalarioBase() { return salarioBase; }
        public String getDeducciones() { return deducciones; }
        public String getNeto()        { return neto; }
    }
}
