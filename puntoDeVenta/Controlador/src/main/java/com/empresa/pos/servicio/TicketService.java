package com.empresa.pos.servicio;

import com.empresa.pos.dao.InventarioDAO;
import com.empresa.pos.dao.ProductoDAO;
import com.empresa.pos.dao.VentaDAO;
import com.empresa.pos.modelo.CartItem;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera recibos de venta en formato TXT, HTML y exportaciones CSV.
 */
public class TicketService {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String LINEA = "=" .repeat(44);
    private static final String LINEA_S = "-".repeat(44);

    // ─────────────────────────── TXT ────────────────────────────

    public static String generarTicketTxt(int ventaId,
                                          ObservableList<CartItem> items,
                                          BigDecimal subtotal,
                                          BigDecimal iva,
                                          BigDecimal total,
                                          int turnoId,
                                          int cajeroId) {
        StringBuilder sb = new StringBuilder();
        String fecha = LocalDateTime.now().format(FMT_FECHA);

        sb.append(LINEA).append("\n");
        sb.append(centrar("POS EMPRESARIAL ERP", 44)).append("\n");
        sb.append(LINEA).append("\n");
        sb.append(String.format("Venta #: %-35d%n", ventaId));
        sb.append(String.format("Turno  : %-35d%n", turnoId));
        sb.append(String.format("Fecha  : %-35s%n", fecha));
        sb.append(LINEA_S).append("\n");
        sb.append(String.format("%-22s %6s %14s%n", "PRODUCTO", "CANT", "SUBTOTAL"));
        sb.append(LINEA_S).append("\n");

        for (CartItem item : items) {
            String nombre = truncar(item.getProducto(), 22);
            sb.append(String.format("%-22s %6.0f %14.2f%n",
                    nombre, item.getCantidad(), item.getSubtotal()));
        }

        sb.append(LINEA_S).append("\n");
        sb.append(String.format("%-30s %13.2f%n", "Subtotal:", subtotal));
        sb.append(String.format("%-30s %13.2f%n", "IVA (16%):", iva));
        sb.append(String.format("%-30s %13.2f%n", "TOTAL:", total));
        sb.append(LINEA).append("\n");
        sb.append(centrar("Gracias por su compra", 44)).append("\n");
        sb.append(LINEA).append("\n");

        return sb.toString();
    }

    // ─────────────────────────── HTML (recibo imprimible) ────────

    public static byte[] generarTicketPdf(int ventaId,
                                          ObservableList<CartItem> items,
                                          BigDecimal subtotal,
                                          BigDecimal iva,
                                          BigDecimal total,
                                          int turnoId,
                                          int cajeroId) {
        String fecha = LocalDateTime.now().format(FMT_FECHA);
        StringBuilder filas = new StringBuilder();
        for (CartItem item : items) {
            filas.append("<tr>")
                 .append("<td>").append(item.getProducto()).append("</td>")
                 .append("<td style='text-align:center'>").append((int) item.getCantidad()).append("</td>")
                 .append("<td style='text-align:right'>$").append(String.format("%.2f", item.getPrecio())).append("</td>")
                 .append("<td style='text-align:right'>$").append(String.format("%.2f", item.getSubtotal())).append("</td>")
                 .append("</tr>\n");
        }

        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8">
                <title>Ticket #%d</title>
                <style>
                  body{font-family:monospace;max-width:420px;margin:auto;padding:20px}
                  h2{text-align:center;border-bottom:2px solid #1A2D5A;padding-bottom:8px;color:#1A2D5A}
                  table{width:100%%;border-collapse:collapse;margin:12px 0}
                  th{background:#1A2D5A;color:white;padding:6px}
                  td{padding:5px;border-bottom:1px solid #ddd}
                  .totales{margin-top:12px;text-align:right;font-size:14px}
                  .total-final{font-size:20px;font-weight:bold;color:#1A2D5A}
                  .footer{text-align:center;margin-top:16px;color:#666;font-size:12px}
                </style>
                </head>
                <body>
                <h2>POS EMPRESARIAL ERP</h2>
                <p><b>Venta #:</b> %d &nbsp;&nbsp; <b>Turno:</b> %d</p>
                <p><b>Fecha:</b> %s</p>
                <table>
                  <thead><tr><th>Producto</th><th>Cant</th><th>Precio</th><th>Subtotal</th></tr></thead>
                  <tbody>%s</tbody>
                </table>
                <div class="totales">
                  <p>Subtotal: $%.2f</p>
                  <p>IVA (16%%): $%.2f</p>
                  <p class="total-final">TOTAL: $%.2f</p>
                </div>
                <div class="footer"><p>Gracias por su compra</p></div>
                </body></html>
                """.formatted(ventaId, ventaId, turnoId, fecha, filas,
                              subtotal, iva, total);

        return html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ─────────────────────────── CSV ────────────────────────────

    public static String generarCsvVentas(List<VentaDAO.Venta> ventas) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Cajero,Subtotal,IVA,Total,Metodo Pago,Estado,Fecha\n");
        for (VentaDAO.Venta v : ventas) {
            sb.append(v.getId()).append(",")
              .append(csvVal(v.getCajeroNombre())).append(",")
              .append(v.getSubtotal()).append(",")
              .append(v.getIva()).append(",")
              .append(v.getTotal()).append(",")
              .append(csvVal(v.getMetodoPagoNombre())).append(",")
              .append(csvVal(v.getEstado())).append(",")
              .append(v.getCreadoEn() != null ? v.getCreadoEn().format(FMT_FECHA) : "").append("\n");
        }
        return sb.toString();
    }

    public static String generarCsvProductos(List<ProductoDAO.Producto> productos) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,SKU,Nombre,Marca,Categoria,Precio,Unidad,Stock Actual,Stock Minimo,Activo\n");
        for (ProductoDAO.Producto p : productos) {
            sb.append(p.getId()).append(",")
              .append(csvVal(p.getSku())).append(",")
              .append(csvVal(p.getNombre())).append(",")
              .append(csvVal(p.getMarca())).append(",")
              .append(csvVal(p.getCategoria())).append(",")
              .append(p.getPrecio()).append(",")
              .append(csvVal(p.getUnidad())).append(",")
              .append(p.getStockActual()).append(",")
              .append(p.getStockMinimo()).append(",")
              .append(p.isActivo()).append("\n");
        }
        return sb.toString();
    }

    public static String generarCsvInventario(List<InventarioDAO.InventarioRow> inventario) {
        StringBuilder sb = new StringBuilder();
        sb.append("Producto ID,SKU,Nombre,Stock Actual,Stock Minimo,Estado\n");
        for (InventarioDAO.InventarioRow row : inventario) {
            String estado = row.getStockActual() <= row.getStockMinimo() ? "CRITICO" : "OK";
            sb.append(row.getProductoId()).append(",")
              .append(csvVal(row.getSku())).append(",")
              .append(csvVal(row.getNombre())).append(",")
              .append(row.getStockActual()).append(",")
              .append(row.getStockMinimo()).append(",")
              .append(estado).append("\n");
        }
        return sb.toString();
    }

    // ─────────────────────────── Utilidades ─────────────────────

    private static String centrar(String texto, int ancho) {
        int espacios = (ancho - texto.length()) / 2;
        return " ".repeat(Math.max(0, espacios)) + texto;
    }

    private static String truncar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + ".";
    }

    private static String csvVal(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
