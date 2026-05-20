package com.empresa.pos.controller.admin;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.InventarioDAO;
import com.empresa.pos.dao.VentaDAO;
import javafx.collections.FXCollections;
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

public class MonitorController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(MonitorController.class);

    @FXML private Label lblVentasGlobales;
    @FXML private Label lblCortes;
    @FXML private Label lblTurnos;

    @FXML private TableView<AlertaInventario> tblAlertas;
    @FXML private TableColumn<AlertaInventario, String>  colAlertaSku;
    @FXML private TableColumn<AlertaInventario, String>  colAlertaNombre;
    @FXML private TableColumn<AlertaInventario, String>  colAlertaCategoria;
    @FXML private TableColumn<AlertaInventario, Integer> colAlertaStock;
    @FXML private TableColumn<AlertaInventario, Integer> colAlertaMinimo;

    @FXML private TableView<CorteHistorico> tblCortes;
    @FXML private TableColumn<CorteHistorico, String> colCajero;
    @FXML private TableColumn<CorteHistorico, String> colFondoInicial;
    @FXML private TableColumn<CorteHistorico, String> colEfectivo;
    @FXML private TableColumn<CorteHistorico, String> colTarjeta;
    @FXML private TableColumn<CorteHistorico, String> colVentaTotal;
    @FXML private TableColumn<CorteHistorico, String> colEstado;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colAlertaSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colAlertaNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colAlertaCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colAlertaStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colAlertaMinimo.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));

        colCajero.setCellValueFactory(new PropertyValueFactory<>("cajero"));
        colFondoInicial.setCellValueFactory(new PropertyValueFactory<>("fondoInicial"));
        colEfectivo.setCellValueFactory(new PropertyValueFactory<>("efectivo"));
        colTarjeta.setCellValueFactory(new PropertyValueFactory<>("tarjeta"));
        colVentaTotal.setCellValueFactory(new PropertyValueFactory<>("ventaTotal"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        cargarDatos();
    }

    private void cargarDatos() {
        try {
            AppContext ctx = AppContext.getInstance();
            VentaDAO ventaDAO = ctx.ventaDAO();
            InventarioDAO invDAO = ctx.inventarioDAO();

            lblVentasGlobales.setText(String.format("$%.2f", ventaDAO.getVentasGlobales()));
            lblCortes.setText(String.valueOf(ventaDAO.getCountCortes()));
            lblTurnos.setText(String.valueOf(ventaDAO.getCountTurnosActivos()));

            List<AlertaInventario> alertas = new ArrayList<>();
            for (InventarioDAO.InventarioRow row : invDAO.findBajoStock()) {
                alertas.add(new AlertaInventario(
                        row.getSku(), row.getNombre(), "",
                        row.getStockActual(), row.getStockMinimo()));
            }
            tblAlertas.setItems(FXCollections.observableArrayList(alertas));

            List<CorteHistorico> cortes = new ArrayList<>();
            for (com.empresa.pos.dao.TurnoDAO.Turno t : ctx.turnoDAO().findAll()) {
                cortes.add(new CorteHistorico(
                    t.getCajeroNombre(),
                    t.getFondoInicial() != null ? String.format("$%.2f", t.getFondoInicial()) : "$0.00",
                    t.getEfectivoIngresado() != null ? String.format("$%.2f", t.getEfectivoIngresado()) : "$0.00",
                    t.getCobtoTarjeta() != null ? String.format("$%.2f", t.getCobtoTarjeta()) : "$0.00",
                    t.getVentaTotal() != null ? String.format("$%.2f", t.getVentaTotal()) : "$0.00",
                    t.getEstado()
                ));
            }
            tblCortes.setItems(FXCollections.observableArrayList(cortes));

        } catch (Exception e) {
            log.warn("No se pudieron cargar datos del monitor: {}", e.getMessage());
            lblVentasGlobales.setText("$0.00");
            lblCortes.setText("0");
            lblTurnos.setText("0");
            tblAlertas.setItems(FXCollections.observableArrayList());
            tblCortes.setItems(FXCollections.observableArrayList());
        }
    }

    // --- Modelos de presentacion ---

    public static class AlertaInventario {
        private final String sku, nombre, categoria;
        private final int stockActual, stockMinimo;

        public AlertaInventario(String sku, String nombre, String categoria,
                                int stockActual, int stockMinimo) {
            this.sku = sku; this.nombre = nombre; this.categoria = categoria;
            this.stockActual = stockActual; this.stockMinimo = stockMinimo;
        }

        public String getSku()        { return sku; }
        public String getNombre()     { return nombre; }
        public String getCategoria()  { return categoria; }
        public int getStockActual()   { return stockActual; }
        public int getStockMinimo()   { return stockMinimo; }
    }

    public static class CorteHistorico {
        private final String cajero, fondoInicial, efectivo, tarjeta, ventaTotal, estado;

        public CorteHistorico(String cajero, String fondoInicial, String efectivo,
                              String tarjeta, String ventaTotal, String estado) {
            this.cajero = cajero; this.fondoInicial = fondoInicial;
            this.efectivo = efectivo; this.tarjeta = tarjeta;
            this.ventaTotal = ventaTotal; this.estado = estado;
        }

        public String getCajero()       { return cajero; }
        public String getFondoInicial() { return fondoInicial; }
        public String getEfectivo()     { return efectivo; }
        public String getTarjeta()      { return tarjeta; }
        public String getVentaTotal()   { return ventaTotal; }
        public String getEstado()       { return estado; }
    }
}
