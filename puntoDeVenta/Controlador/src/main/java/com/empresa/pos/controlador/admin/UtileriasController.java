package com.empresa.pos.controlador.admin;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.ClienteDAO;
import com.empresa.pos.dao.ProductoDAO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UtileriasController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(UtileriasController.class);

    @FXML private Label lblVersion;
    @FXML private Label lblBaseDatos;
    @FXML private Label lblConexion;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblVersion.setText("1.0.0-SNAPSHOT");
        lblBaseDatos.setText("PostgreSQL");
        verificarConexion();
    }

    private void verificarConexion() {
        try {
            AppContext.getInstance().productoDAO();
            lblConexion.setText("Conectado");
            lblConexion.setStyle("-fx-text-fill: #388E3C; -fx-font-weight: bold;");
        } catch (Exception e) {
            lblConexion.setText("Sin conexion");
            lblConexion.setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleExportarProductos() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Productos CSV");
        fc.setInitialFileName("productos_export.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = fc.showSaveDialog(lblVersion.getScene().getWindow());
        if (file == null) return;

        try {
            List<ProductoDAO.Producto> lista = AppContext.getInstance().productoDAO().findAll();
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                w.write("ID,SKU,Nombre,Marca,Categoria,Precio,Stock,Unidad,Activo");
                w.newLine();
                for (ProductoDAO.Producto p : lista) {
                    w.write(String.join(",",
                            String.valueOf(p.getId()),
                            esc(p.getSku()),
                            esc(p.getNombre()),
                            esc(p.getMarca()),
                            esc(p.getCategoria()),
                            p.getPrecio() != null ? p.getPrecio().toPlainString() : "0",
                            String.valueOf(p.getStockActual()),
                            esc(p.getUnidad()),
                            String.valueOf(p.isActivo())
                    ));
                    w.newLine();
                }
            }
            mostrarInfo("Exportacion completada",
                    "Productos exportados: " + lista.size() + "\nArchivo: " + file.getAbsolutePath());
            log.info("Productos exportados a {}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("Error al exportar productos: {}", e.getMessage());
            mostrarError("No se pudo exportar: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportarClientes() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Clientes CSV");
        fc.setInitialFileName("clientes_export.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = fc.showSaveDialog(lblVersion.getScene().getWindow());
        if (file == null) return;

        try {
            List<ClienteDAO.Cliente> lista = AppContext.getInstance().clienteDAO().findAll();
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                w.write("ID,Codigo,Nombre,RFC,Tipo,Puntos");
                w.newLine();
                for (ClienteDAO.Cliente c : lista) {
                    w.write(String.join(",",
                            String.valueOf(c.getId()),
                            esc(c.getCodigo()),
                            esc(c.getNombre()),
                            esc(c.getRfc()),
                            esc(c.getTipo()),
                            String.valueOf(c.getPuntosAcumulados())
                    ));
                    w.newLine();
                }
            }
            mostrarInfo("Exportacion completada",
                    "Clientes exportados: " + lista.size() + "\nArchivo: " + file.getAbsolutePath());
            log.info("Clientes exportados a {}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("Error al exportar clientes: {}", e.getMessage());
            mostrarError("No se pudo exportar: " + e.getMessage());
        }
    }

    @FXML
    private void handleSeed() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Seed de Datos");
        confirm.setHeaderText("Insertar datos de prueba");
        confirm.setContentText("Se insertaran datos de ejemplo solo si la BD esta vacia.\n¿Continuar?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn.getButtonData().isDefaultButton()) {
                mostrarInfo("Seed", "La funcion de seed debe ejecutarse desde el panel de administracion del servidor.");
            }
        });
    }

    private String esc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    private void mostrarInfo(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void mostrarError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
