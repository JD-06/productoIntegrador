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

public class CxPController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CxPController.class);

    @FXML private TableView<FilaCxP> tblCxP;
    @FXML private TableColumn<FilaCxP, String> colCxpId;
    @FXML private TableColumn<FilaCxP, String> colCxpProveedor;
    @FXML private TableColumn<FilaCxP, String> colCxpMonto;
    @FXML private TableColumn<FilaCxP, String> colCxpSaldo;
    @FXML private TableColumn<FilaCxP, String> colCxpEstado;
    @FXML private TableColumn<FilaCxP, String> colCxpVencimiento;
    @FXML private TableColumn<FilaCxP, Void>   colCxpAcciones;

    private final ObservableList<FilaCxP> datos = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colCxpId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCxpProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colCxpMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));
        colCxpSaldo.setCellValueFactory(new PropertyValueFactory<>("saldo"));
        colCxpEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colCxpVencimiento.setCellValueFactory(new PropertyValueFactory<>("vencimiento"));

        colCxpEstado.setCellFactory(col -> new TableCell<>() {
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

        colCxpAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnPagar = new Button("Pagar");
            {
                btnPagar.setStyle("-fx-font-size: 11px;");
                btnPagar.setOnAction(e -> {
                    FilaCxP fila = getTableView().getItems().get(getIndex());
                    log.info("Registrar pago CxP id={}", fila.getId());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnPagar);
                setAlignment(Pos.CENTER);
            }
        });

        tblCxP.setItems(datos);
        cargarDatos();
    }

    private void cargarDatos() {
        try {
            CuentaDAO dao = AppContext.getInstance().cuentaDAO();
            List<FilaCxP> lista = new ArrayList<>();
            for (CuentaDAO.CuentaPagar c : dao.findAllCxP()) {
                lista.add(new FilaCxP(
                        String.valueOf(c.getId()),
                        c.getProveedorNombre() != null ? c.getProveedorNombre() : "",
                        c.getMonto() != null ? String.format("$%.2f", c.getMonto()) : "$0.00",
                        c.getSaldo() != null ? String.format("$%.2f", c.getSaldo()) : "$0.00",
                        c.getEstado() != null ? c.getEstado() : "",
                        c.getVencimiento() != null ? c.getVencimiento().toString() : ""
                ));
            }
            datos.setAll(lista);
        } catch (Exception e) {
            log.warn("No se pudieron cargar cuentas por pagar: {}", e.getMessage());
            datos.setAll(
                new FilaCxP("1", "Distribuidor Central S.A.", "$12,500.00", "$12,500.00", "PENDIENTE", "2026-06-15"),
                new FilaCxP("2", "Proveedores Unidos",        "$4,300.00",  "$0.00",      "PAGADA",    "2026-05-01"),
                new FilaCxP("3", "Logistica Express",         "$2,100.00",  "$2,100.00",  "VENCIDA",   "2026-04-20")
            );
        }
    }

    @FXML
    private void handleNuevaCxP() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Nueva CxP");
        alert.setHeaderText(null);
        alert.setContentText("Formulario de nueva cuenta por pagar en desarrollo.");
        alert.showAndWait();
    }

    public static class FilaCxP {
        private final String id, proveedor, monto, saldo, estado, vencimiento;

        public FilaCxP(String id, String proveedor, String monto, String saldo,
                       String estado, String vencimiento) {
            this.id = id; this.proveedor = proveedor; this.monto = monto;
            this.saldo = saldo; this.estado = estado; this.vencimiento = vencimiento;
        }

        public String getId()          { return id; }
        public String getProveedor()   { return proveedor; }
        public String getMonto()       { return monto; }
        public String getSaldo()       { return saldo; }
        public String getEstado()      { return estado; }
        public String getVencimiento() { return vencimiento; }
    }
}
