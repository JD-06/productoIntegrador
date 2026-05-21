using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PosMaui.Modelo.Dao;
using PosMaui.Modelo.Entidad;

namespace PosMaui.Controlador.Admin;

public partial class AdminViewModel : ObservableObject
{
    [ObservableProperty] private string _nombreAdmin = "";
    [ObservableProperty] private decimal _ventasGlobales;
    [ObservableProperty] private int _turnosActivos;
    [ObservableProperty] private int _cortes;
    [ObservableProperty] private List<Producto> _alertasStock = [];
    [ObservableProperty] private List<Venta> _historialVentas = [];
    [ObservableProperty] private List<Producto> _catalogo = [];
    [ObservableProperty] private List<Producto> _inventario = [];
    [ObservableProperty] private List<Usuario> _usuarios = [];
    [ObservableProperty] private string _busquedaCatalogo = "";
    [ObservableProperty] private string _estado = "";

    private List<Producto> _todosProductos = [];

    public void Inicializar(string nombre)
    {
        NombreAdmin = nombre;
        CargarDashboard();
    }

    [RelayCommand]
    public void CargarDashboard()
    {
        try
        {
            VentasGlobales = VentaDao.VentasGlobales();
            TurnosActivos  = VentaDao.ContarTurnosActivos();
            Cortes         = VentaDao.ContarCortes();
            AlertasStock   = ProductoDao.ObtenerBajoStock();
            HistorialVentas = VentaDao.ObtenerTodas();
        }
        catch (Exception ex) { Estado = ex.Message; }
    }

    [RelayCommand]
    public void CargarCatalogo()
    {
        try
        {
            _todosProductos = ProductoDao.ObtenerTodos();
            Catalogo = _todosProductos;
        }
        catch (Exception ex) { Estado = ex.Message; }
    }

    partial void OnBusquedaCatalogoChanged(string value)
    {
        if (string.IsNullOrWhiteSpace(value))
            Catalogo = _todosProductos;
        else
            Catalogo = _todosProductos
                .Where(p => p.Nombre.Contains(value, StringComparison.OrdinalIgnoreCase)
                         || p.Sku.Contains(value, StringComparison.OrdinalIgnoreCase))
                .ToList();
    }

    [RelayCommand]
    public void CargarInventario()
    {
        try { Inventario = ProductoDao.ObtenerBajoStock(); }
        catch (Exception ex) { Estado = ex.Message; }
    }

    [RelayCommand]
    public void CargarUsuarios()
    {
        try { Usuarios = UsuarioDao.ObtenerActivos(); }
        catch (Exception ex) { Estado = ex.Message; }
    }
}
