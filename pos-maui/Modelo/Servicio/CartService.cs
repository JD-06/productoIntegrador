namespace PosMaui.Modelo.Servicio;

public static class CartService
{
    private const decimal IVA = 0.16m;

    public static decimal CalcularSubtotal(IEnumerable<decimal> subtotales)
        => subtotales.Sum();

    public static decimal CalcularIva(decimal subtotal)
        => subtotal * IVA;

    public static decimal CalcularTotal(decimal subtotal)
        => subtotal + CalcularIva(subtotal);
}
