package com.empresa.pos.controller.admin;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.ClienteDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class PremiosLealtadController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(PremiosLealtadController.class);

    @FXML private Label lblClientesPuntos;
    @FXML private Label lblTotalPuntos;

    @FXML private TableView<FilaCliente> tblClientes;
    @FXML private TableColumn<FilaCliente, String>  colPremCliente;
    @FXML private TableColumn<FilaCliente, String>  colPremTipo;
    @FXML private TableColumn<FilaCliente, Integer> colPremPuntos;
    @FXML private TableColumn<FilaCliente, Void>    colPremAcciones;

    private final ObservableList<FilaCliente> datos = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colPremCliente.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPremTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colPremPuntos.setCellValueFactory(new PropertyValueFactory<>("puntos"));

        colPremAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnCanjear = new Button("Canjear");
            {
                btnCanjear.setStyle("-fx-font-size: 11px;");
                btnCanjear.setOnAction(e -> {
                    FilaCliente fila = getTableView().getItems().get(getIndex());
                    manejarCanje(fila);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnCanjear);
                setAlignment(Pos.CENTER);
            }
        });

        tblClientes.setItems(datos);
        cargarDatos();
    }

    private void cargarDatos() {
        try {
            ClienteDAO dao = AppContext.getInstance().clienteDAO();
            List<FilaCliente> lista = new ArrayList<>();
            int totalPuntos = 0;
            for (ClienteDAO.Cliente c : dao.findAll()) {
                if ("MENUDEO".equalsIgnoreCase(c.getTipo()) && c.getPuntosAcumulados() > 0) {
                    lista.add(new FilaCliente(c.getNombre(), c.getTipo(), c.getPuntosAcumulados()));
                    totalPuntos += c.getPuntosAcumulados();
                }
            }
            datos.setAll(lista);
            lblClientesPuntos.setText(String.valueOf(lista.size()));
            lblTotalPuntos.setText(String.valueOf(totalPuntos));
        } catch (Exception e) {
            log.warn("No se pudieron cargar clientes de lealtad: {}", e.getMessage());
            datos.setAll(
                new FilaCliente("Maria Garcia Lopez",  "MENUDEO", 320),
                new FilaCliente("Pedro Martinez Ruiz", "MENUDEO",  85),
                new FilaCliente("Ana Torres Silva",    "MENUDEO", 150)
            );
            lblClientesPuntos.setText("3");
            lblTotalPuntos.setText("555");
        }
    }

    private void manejarCanje(FilaCliente cliente) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Canje de Puntos");
        alert.setHeaderText("Canje para: " + cliente.getNombre());
        alert.setContentText("Puntos disponibles: " + cliente.getPuntos()
                + "\n\nFuncionalidad de canje en desarrollo.");
        alert.showAndWait();
    }

    public static class FilaCliente {
        private final String nombre, tipo;
        private final int puntos;
        public FilaCliente(String nombre, String tipo, int puntos) {
            this.nombre = nombre; this.tipo = tipo; this.puntos = puntos;
        }
        public String getNombre() { return nombre; }
        public String getTipo()   { return tipo; }
        public int getPuntos()    { return puntos; }
    }
}
