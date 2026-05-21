namespace PosMaui.Modelo.Entidad;

public class CartItem
{
    public int     DbProductoId { get; }
    public string  Nombre       { get; }
    public decimal Precio       { get; }
    public double  Cantidad     { get; set; }
    public decimal Subtotal     => Precio * (decimal)Cantidad;

    public CartItem(int dbProductoId, string nombre, double cantidad, decimal precio)
    {
        DbProductoId = dbProductoId;
        Nombre       = nombre;
        Cantidad     = cantidad;
        Precio       = precio;
    }
}
