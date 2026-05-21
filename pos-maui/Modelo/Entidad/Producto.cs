namespace PosMaui.Modelo.Entidad;

public class Producto
{
    public int     Id          { get; set; }
    public string  Sku         { get; set; } = "";
    public string  Nombre      { get; set; } = "";
    public string  Marca       { get; set; } = "";
    public string  Categoria   { get; set; } = "";
    public int     CategoriaId { get; set; }
    public decimal Precio      { get; set; }
    public string  Unidad      { get; set; } = "";
    public int     StockActual { get; set; }
    public int     StockMinimo { get; set; }
    public bool    Activo      { get; set; } = true;
    public string? ImagenLocal { get; set; }

    public string PrecioFormateado => $"${Precio:F2}";
    public bool   BajoStock       => StockActual <= StockMinimo;
}
