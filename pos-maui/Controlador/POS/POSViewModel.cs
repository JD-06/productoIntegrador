using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PosMaui.Modelo.Dao;
using PosMaui.Modelo.Entidad;
using PosMaui.Modelo.Servicio;

namespace PosMaui.Controlador.POS;

public partial class POSViewModel : ObservableObject
{
    [ObservableProperty] private List<Producto> _catalogoVisible = [];
    [ObservableProperty] private List<CartItem> _carrito = [];
    [ObservableProperty] private string _busqueda = "";
    [ObservableProperty] private decimal _subtotal;
    [ObservableProperty] private decimal _iva;
    [ObservableProperty] private decimal _total;
    [ObservableProperty] private string _labelCajero = "";
    [ObservableProperty] private string _labelTurno = "";
    [ObservableProperty] private string _estado = "";

    private List<Producto> _todosProductos = [];
    private int _cajeroId;
    private int _turnoId;
    private string _cajeroNombre = "";

    public event Func<CartItem[], decimal, decimal, decimal, int, int, Task>? AbrirCobro;

    public void Inicializar(int cajeroId, string cajeroNombre, int turnoId)
    {
        _cajeroId = cajeroId;
        _turnoId  = turnoId;
        _cajeroNombre = cajeroNombre;
        LabelCajero = cajeroNombre;
        LabelTurno  = $"Turno #{turnoId}";
        CargarProductos();
    }

    private void CargarProductos()
    {
        try
        {
            _todosProductos = ProductoDao.ObtenerTodos();
            CatalogoVisible = _todosProductos.Take(60).ToList();
            Estado = $"{_todosProductos.Count} productos cargados";
        }
        catch (Exception ex)
        {
            Estado = "Error cargando productos: " + ex.Message;
        }
    }

    partial void OnBusquedaChanged(string value)
    {
        if (string.IsNullOrWhiteSpace(value))
            CatalogoVisible = _todosProductos.Take(60).ToList();
        else
            CatalogoVisible = _todosProductos
                .Where(p => p.Nombre.Contains(value, StringComparison.OrdinalIgnoreCase)
                         || p.Sku.Contains(value, StringComparison.OrdinalIgnoreCase))
                .Take(60).ToList();
    }

    [RelayCommand]
    public void AgregarAlCarrito(Producto producto)
    {
        if (producto.StockActual <= 0)
        {
            Estado = $"Sin stock: {producto.Nombre}"; return;
        }

        var existente = Carrito.FirstOrDefault(c => c.DbProductoId == producto.Id);
        if (existente != null)
            existente.Cantidad++;
        else
            Carrito = [.. Carrito, new CartItem(producto.Id, producto.Nombre, 1, producto.Precio)];

        Recalcular();
        Estado = $"Agregado: {producto.Nombre}";
    }

    [RelayCommand]
    public void EliminarItem(CartItem item)
    {
        Carrito = Carrito.Where(c => c != item).ToList();
        Recalcular();
    }

    [RelayCommand]
    public void CancelarVenta()
    {
        Carrito = [];
        Recalcular();
        Estado = "Venta cancelada";
    }

    [RelayCommand]
    public async Task Cobrar()
    {
        if (Carrito.Count == 0) { Estado = "El carrito está vacío."; return; }
        if (AbrirCobro != null)
            await AbrirCobro.Invoke([.. Carrito], Subtotal, Iva, Total, _cajeroId, _turnoId);
    }

    public void LimpiarCarrito()
    {
        Carrito = [];
        Recalcular();
        Estado = "Venta completada";
    }

    private void Recalcular()
    {
        Subtotal = CartService.CalcularSubtotal(Carrito.Select(c => c.Subtotal));
        Iva      = CartService.CalcularIva(Subtotal);
        Total    = CartService.CalcularTotal(Subtotal);
    }
}
