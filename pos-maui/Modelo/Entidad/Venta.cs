namespace PosMaui.Modelo.Entidad;

public class Venta
{
    public int      Id               { get; set; }
    public int      TurnoId          { get; set; }
    public int      CajeroId         { get; set; }
    public string   CajeroNombre     { get; set; } = "";
    public decimal  Subtotal         { get; set; }
    public decimal  Iva              { get; set; }
    public decimal  Total            { get; set; }
    public string   MetodoPagoNombre { get; set; } = "";
    public decimal? MontoRecibido    { get; set; }
    public decimal? Cambio           { get; set; }
    public string   Estado           { get; set; } = "COMPLETADA";
    public DateTime CreadoEn         { get; set; } = DateTime.Now;
}
