using PosMaui.Modelo.Entidad;

namespace PosMaui.Modelo.Servicio;

public static class TicketService
{
    public static string GenerarTxt(int ventaId, IList<CartItem> items,
                                     decimal subtotal, decimal iva, decimal total,
                                     string cajero, string metodo,
                                     decimal montoRecibido, decimal cambio)
    {
        var sb = new System.Text.StringBuilder();
        string linea  = new('=', 44);
        string lineaS = new('-', 44);
        string fecha  = DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss");

        sb.AppendLine(linea);
        sb.AppendLine(Centrar("POS EMPRESARIAL ERP", 44));
        sb.AppendLine(linea);
        sb.AppendLine($"Venta #: {ventaId,-35}");
        sb.AppendLine($"Cajero : {cajero,-35}");
        sb.AppendLine($"Fecha  : {fecha,-35}");
        sb.AppendLine(lineaS);
        sb.AppendLine($"{"PRODUCTO",-22} {"CANT",6} {"SUBTOTAL",14}");
        sb.AppendLine(lineaS);

        foreach (var item in items)
        {
            string nombre = Truncar(item.Nombre, 22);
            sb.AppendLine($"{nombre,-22} {(int)item.Cantidad,6} {item.Subtotal,14:F2}");
        }

        sb.AppendLine(lineaS);
        sb.AppendLine($"{"Subtotal:",-30} {subtotal,13:F2}");
        sb.AppendLine($"{"IVA (16%):",-30} {iva,13:F2}");
        sb.AppendLine($"{"TOTAL:",-30} {total,13:F2}");
        sb.AppendLine(lineaS);
        sb.AppendLine($"{"Metodo:",-30} {metodo,-13}");
        if (metodo == "EFECTIVO")
        {
            sb.AppendLine($"{"Recibido:",-30} {montoRecibido,13:F2}");
            sb.AppendLine($"{"Cambio:",-30} {cambio,13:F2}");
        }
        sb.AppendLine(linea);
        sb.AppendLine(Centrar("Gracias por su compra", 44));
        sb.AppendLine(linea);

        return sb.ToString();
    }

    public static string GenerarHtml(int ventaId, IList<CartItem> items,
                                      decimal subtotal, decimal iva, decimal total,
                                      string cajero, string metodo)
    {
        var sb2 = new System.Text.StringBuilder();
        foreach (var i in items)
            sb2.Append($"<tr><td>{i.Nombre}</td><td style=\"text-align:center\">{(int)i.Cantidad}</td>"
                     + $"<td style=\"text-align:right\">${i.Precio:F2}</td>"
                     + $"<td style=\"text-align:right\">${i.Subtotal:F2}</td></tr>");

        string css = "body{font-family:monospace;max-width:420px;margin:auto;padding:20px}"
                   + "h2{text-align:center;border-bottom:2px solid #1A2D5A;color:#1A2D5A}"
                   + "table{width:100%;border-collapse:collapse;margin:12px 0}"
                   + "th{background:#1A2D5A;color:white;padding:6px}"
                   + "td{padding:5px;border-bottom:1px solid #ddd}"
                   + ".total-final{font-size:20px;font-weight:bold;color:#1A2D5A}";

        return $"<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
             + $"<title>Ticket #{ventaId}</title><style>{css}</style></head><body>"
             + $"<h2>POS EMPRESARIAL ERP</h2>"
             + $"<p><b>Venta #:</b> {ventaId} <b>Cajero:</b> {cajero}</p>"
             + $"<p><b>Fecha:</b> {DateTime.Now:dd/MM/yyyy HH:mm:ss}</p>"
             + $"<table><thead><tr><th>Producto</th><th>Cant</th><th>Precio</th><th>Subtotal</th></tr></thead>"
             + $"<tbody>{sb2}</tbody></table>"
             + $"<div style=\"text-align:right;margin-top:12px\">"
             + $"<p>Subtotal: ${subtotal:F2}</p><p>IVA (16%): ${iva:F2}</p>"
             + $"<p style=\"font-size:20px;font-weight:bold;color:#1A2D5A\">TOTAL: ${total:F2}</p>"
             + $"<p>Metodo: {metodo}</p></div>"
             + "<div style=\"text-align:center;color:#666\">Gracias por su compra</div>"
             + "</body></html>";
    }

    private static string Centrar(string texto, int ancho)
    {
        int espacios = (ancho - texto.Length) / 2;
        return new string(' ', Math.Max(0, espacios)) + texto;
    }

    private static string Truncar(string texto, int max)
        => texto.Length <= max ? texto : texto[..(max - 1)] + ".";
}
