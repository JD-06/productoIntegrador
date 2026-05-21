using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PosMaui.Modelo.Dao;
using PosMaui.Modelo.Entidad;

namespace PosMaui.Controlador;

public partial class LoginViewModel : ObservableObject
{
    [ObservableProperty] private List<string> _usuarios = [];
    [ObservableProperty] private string _usuarioSeleccionado = "";
    [ObservableProperty] private string _pin = "";
    [ObservableProperty] private string _fondoInicial = "";
    [ObservableProperty] private string _error = "";
    [ObservableProperty] private bool _mostrarFondo = true;

    private List<Usuario> _listaUsuarios = [];

    public event Action<Usuario, int>? NavegaAlPOS;
    public event Action<Usuario>?      NavegaAlAdmin;

    public LoginViewModel() => CargarUsuarios();

    private void CargarUsuarios()
    {
        try
        {
            _listaUsuarios = UsuarioDao.ObtenerActivos();
            Usuarios = _listaUsuarios.Select(u => u.Nombre).ToList();
        }
        catch (Exception ex)
        {
            Error = "Sin conexion a la base de datos: " + ex.Message;
        }
    }

    partial void OnUsuarioSeleccionadoChanged(string value)
    {
        var u = _listaUsuarios.FirstOrDefault(x => x.Nombre == value);
        MostrarFondo = u != null && !u.EsAdmin;
    }

    [RelayCommand]
    public void AbrirTurno()
    {
        Error = "";

        if (string.IsNullOrWhiteSpace(UsuarioSeleccionado) || string.IsNullOrWhiteSpace(Pin))
        {
            Error = "Complete todos los campos."; return;
        }

        var usuario = _listaUsuarios.FirstOrDefault(u => u.Nombre == UsuarioSeleccionado);
        if (usuario == null) { Error = "Usuario no encontrado."; return; }

        if (!usuario.EsAdmin && string.IsNullOrWhiteSpace(FondoInicial))
        {
            Error = "Ingrese el fondo inicial."; return;
        }

        var verificado = UsuarioDao.VerificarPin(UsuarioSeleccionado, Pin);
        if (verificado == null) { Error = "PIN incorrecto."; return; }

        if (verificado.EsAdmin)
        {
            NavegaAlAdmin?.Invoke(verificado);
        }
        else
        {
            decimal fondo = decimal.TryParse(FondoInicial, out var f) ? f : 0;
            int turnoId = VentaDao.AbrirTurno(verificado.Id, fondo);
            NavegaAlPOS?.Invoke(verificado, turnoId);
        }
    }
}
