package com.empresa.pos.service;

import com.empresa.pos.dao.InventarioDAO;
import com.empresa.pos.dao.ProductoDAO;
import com.empresa.pos.dao.VentaDAO;
import com.empresa.pos.model.CartItem;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TicketService {

    private static final String LINEA = "-".repeat(40);
    private static final String DOBLE_LINEA = "=".repeat(40);

    public static String generarTicketTxt(int ventaId, ObservableList<CartItem> items,
                                          BigDecimal subtotal, BigDecimal iva, BigDecimal total,
                                          int turnoId, int cajeroId) {
        StringBuilder sb = new StringBuilder();
        sb.append("      POS EMPRESARIAL ERP\n");
        sb.append("      TICKET DE VENTA\n");
        sb.append(DOBLE_LINEA).append("\n");
        sb.append("Fecha: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .append("\n");
        sb.append("Venta #").append(ventaId).append("\n");
        sb.append("Turno #").append(turnoId).append("  Cajero #").append(cajeroId).append("\n");
        sb.append(LINEA).append("\n");
        sb.append(String.format("%-20s %4s %8s %8s\n", "PRODUCTO", "CANT", "PRECIO", "SUBTOT"));
        sb.append(LINEA).append("\n");
        for (CartItem item : items) {
            String nombre = item.getProducto().length() > 20
                    ? item.getProducto().substring(0, 17) + "..."
                    : item.getProducto();
            sb.append(String.format("%-20s %4.0f %8.2f %8.2f\n",
                    nombre, item.getCantidad(), item.getPrecio(), item.getSubtotal()));
        }
        sb.append(LINEA).append("\n");
        sb.append(String.format("%-32s %8.2f\n", "SUBTOTAL:", subtotal));
        sb.append(String.format("%-32s %8.2f\n", "IVA (16%):", iva));
        sb.append(String.format("%-32s %8.2f\n", "TOTAL:", total));
        sb.append(DOBLE_LINEA).append("\n");
        sb.append("   Gracias por su compra!\n");
        return sb.toString();
    }

    public static byte[] generarTicketPdf(int ventaId, ObservableList<CartItem> items,
                                          BigDecimal subtotal, BigDecimal iva, BigDecimal total,
                                          int turnoId, int cajeroId) {
        List<String> lineas = new ArrayList<>();
        lineas.add("POS EMPRESARIAL ERP");
        lineas.add("TICKET DE VENTA");
        lineas.add("Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        lineas.add("Venta #" + ventaId);
        lineas.add("Turno #" + turnoId + "  Cajero #" + cajeroId);
        lineas.add("");
        for (CartItem item : items) {
            lineas.add(item.getProducto());
            lineas.add(String.format("  %.0f x $%.2f = $%.2f", item.getCantidad(), item.getPrecio(), item.getSubtotal()));
        }
        lineas.add("");
        lineas.add(String.format("SUBTOTAL: $%.2f", subtotal));
        lineas.add(String.format("IVA: $%.2f", iva));
        lineas.add(String.format("TOTAL: $%.2f", total));
        lineas.add("");
        lineas.add("Gracias por su compra!");

        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 10 Tf\n40 780 Td\n");
        boolean first = true;
        for (String linea : lineas) {
            if (!first) {
                stream.append("0 -14 Td\n");
            }
            stream.append("(").append(escapePdf(linea)).append(") Tj\n");
            first = false;
        }
        stream.append("ET\n");

        String content = stream.toString();
        byte[] contentBytes = content.getBytes(StandardCharsets.US_ASCII);

        String obj1 = "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n";
        String obj2 = "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n";
        String obj3 = "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 226 820] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n";
        String obj4a = "4 0 obj << /Length " + contentBytes.length + " >> stream\n";
        String obj4b = "endstream\nendobj\n";
        String obj5 = "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n";

        int offset = 0;
        List<Integer> offsets = new ArrayList<>();
        String header = "%PDF-1.4\n";
        offset += header.getBytes(StandardCharsets.US_ASCII).length;

        offsets.add(offset);
        offset += obj1.getBytes(StandardCharsets.US_ASCII).length;
        offsets.add(offset);
        offset += obj2.getBytes(StandardCharsets.US_ASCII).length;
        offsets.add(offset);
        offset += obj3.getBytes(StandardCharsets.US_ASCII).length;
        offsets.add(offset);
        offset += obj4a.getBytes(StandardCharsets.US_ASCII).length + contentBytes.length + obj4b.getBytes(StandardCharsets.US_ASCII).length;
        offsets.add(offset);
        offset += obj5.getBytes(StandardCharsets.US_ASCII).length;

        StringBuilder pdf = new StringBuilder();
        pdf.append(header);
        pdf.append(obj1);
        pdf.append(obj2);
        pdf.append(obj3);
        pdf.append(obj4a);
        pdf.append(content);
        pdf.append(obj4b);
        pdf.append(obj5);

        int xrefOffset = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
        pdf.append("xref\n0 6\n");
        pdf.append("0000000000 65535 f \n");
        for (Integer current : offsets) {
            pdf.append(String.format("%010d 00000 n \n", current));
        }
        pdf.append("trailer << /Size 6 /Root 1 0 R >>\n");
        pdf.append("startxref\n");
        pdf.append(xrefOffset).append("\n");
        pdf.append("%%EOF");

        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    public static String generarCsvVentas(List<VentaDAO.Venta> ventas) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,turno_id,cajero_id,cliente_id,subtotal,iva,total,metodo_pago_id,monto_recibido,cambio,estado,creado_en\n");
        for (VentaDAO.Venta v : ventas) {
            sb.append(String.format("%d,%d,%d,%s,%.2f,%.2f,%.2f,%d,%.2f,%.2f,%s,%s\n",
                    v.getId(), v.getTurnoId(), v.getCajeroId(),
                    v.getClienteId() > 0 ? String.valueOf(v.getClienteId()) : "",
                    v.getSubtotal(), v.getIva(), v.getTotal(),
                    v.getMetodoPagoId(), v.getMontoRecibido(), v.getCambio(),
                    csv(v.getEstado()),
                    v.getCreadoEn() != null ? csv(v.getCreadoEn().toString()) : ""));
        }
        return sb.toString();
    }

    public static String generarCsvProductos(List<ProductoDAO.Producto> productos) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,sku,nombre,marca,categoria,precio,unidad,stock_actual,stock_minimo,activo\n");
        for (ProductoDAO.Producto p : productos) {
            sb.append(String.format("%d,%s,%s,%s,%s,%.2f,%s,%d,%d,%s\n",
                    p.getId(), csv(p.getSku()), csv(p.getNombre()), csv(p.getMarca()), csv(p.getCategoria()),
                    p.getPrecio(), csv(p.getUnidad()), p.getStockActual(), p.getStockMinimo(), p.isActivo()));
        }
        return sb.toString();
    }

    public static String generarCsvInventario(List<InventarioDAO.InventarioRow> inventario) {
        StringBuilder sb = new StringBuilder();
        sb.append("producto_id,sku,nombre,stock_actual,stock_minimo\n");
        for (InventarioDAO.InventarioRow row : inventario) {
            sb.append(String.format("%d,%s,%s,%d,%d\n",
                    row.getProductoId(), csv(row.getSku()), csv(row.getNombre()), row.getStockActual(), row.getStockMinimo()));
        }
        return sb.toString();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static String escapePdf(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
