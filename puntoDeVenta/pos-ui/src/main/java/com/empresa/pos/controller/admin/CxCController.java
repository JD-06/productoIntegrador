package com.empresa.pos.controller.admin;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.CuentaDAO;
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

public class CxCController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CxCController.class);

    @FXML private TableView<FilaCxC> tblCxC;
    @FXML private TableColumn<FilaCxC, String> colCxcId;
    @FXML private TableColumn<FilaCxC, String> colCxcCliente;
    @FXML private TableColumn<FilaCxC, String> colCxcMonto;
    @FXML private TableColumn<FilaCxC, String> colCxcSaldo;
    @FXML private TableColumn<FilaCxC, String> colCxcEstado;
    @FXML private TableColumn<FilaCxC, String> colCxcVencimiento;
    @FXML private TableColumn<FilaCxC, Void>   colCxcAcciones;

    private final ObservableList<FilaCxC> datos = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colCxcId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCxcCliente.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        colCxcMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));
        colCxcSaldo.setCellValueFactory(new PropertyValueFactory<>("saldo"));
        colCxcEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colCxcVencimiento.setCellValueFactory(new PropertyValueFactory<>("vencimiento"));

        colCxcEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setAlignment(Pos.CENTER);
                setStyle("VENCIDA".equals(item)
                        ? "-fx-text-fill: #D32F2F; -fx-font-weight: bold;"
                        : "PAGADA".equals(item)
                            ? "-fx-text-fill: #388E3C; -fx-font-weight: bold;"
                            : "-fx-text-fill: #F57C00; -fx-font-weight: bold;");
            }
        });

        colCxcAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnPagar = new Button("Pagar");
            {
                btnPagar.setStyle("-fx-font-size: 11px;");
                btnPagar.setOnAction(e -> {
                    FilaCxC fila = getTableView().getItems().get(getIndex());
                    log.info("Registrar pago CxC id={}", fila.getId());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnPagar);
                setAlignment(Pos.CENTER);
            }
        });

        tblCxC.setItems(datos);
        cargarDatos();
    }

    private void cargarDatos() {
        try {
            CuentaDAO dao = AppContext.getInstance().cuentaDAO();
            List<FilaCxC> lista = new ArrayList<>();
            for (CuentaDAO.CuentaCobrar c : dao.findAllCxC()) {
                lista.add(new FilaCxC(
                        String.valueOf(c.getId()),
                        c.getClienteNombre() != null ? c.getClienteNombre() : "",
                        c.getMonto() != null ? String.format("$%.2f", c.getMonto()) : "$0.00",
                        c.getSaldo() != null ? String.format("$%.2f", c.getSaldo()) : "$0.00",
                        c.getEstado() != null ? c.getEstado() : "",
                        c.getVencimiento() != null ? c.getVencimiento().toString() : ""
                ));
            }
            datos.setAll(lista);
        } catch (Exception e) {
            log.warn("No se pudieron cargar cuentas por cobrar: {}", e.getMessage());
            datos.setAll(
                new FilaCxC("1", "Restaurante Los Arcos", "$5,000.00", "$5,000.00", "PENDIENTE", "2026-06-01"),
                new FilaCxC("2", "Tienda El Sol",          "$2,300.00", "$0.00",     "PAGADA",    "2026-05-15"),
                new FilaCxC("3", "Comercial Norte S.A.",   "$8,750.00", "$8,750.00", "VENCIDA",   "2026-04-30")
            );
        }
    }

    @FXML
    private void handleNuevaCxC() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Nueva CxC");
        alert.setHeaderText(null);
        alert.setContentText("Formulario de nueva cuenta por cobrar en desarrollo.");
        alert.showAndWait();
    }

    public static class FilaCxC {
        private final String id, cliente, monto, saldo, estado, vencimiento;

        public FilaCxC(String id, String cliente, String monto, String saldo,
                       String estado, String vencimiento) {
            this.id = id; this.cliente = cliente; this.monto = monto;
            this.saldo = saldo; this.estado = estado; this.vencimiento = vencimiento;
        }

        public String getId()          { return id; }
        public String getCliente()     { return cliente; }
        public String getMonto()       { return monto; }
        public String getSaldo()       { return saldo; }
        public String getEstado()      { return estado; }
        public String getVencimiento() { return vencimiento; }
    }
}
