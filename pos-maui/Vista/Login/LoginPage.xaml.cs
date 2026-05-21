using PosMaui.Controlador;
using PosMaui.Modelo.Dao;
using PosMaui.Modelo.Entidad;
using PosMaui.Vista.Admin;
using PosMaui.Vista.POS;

namespace PosMaui.Vista.Login;

public partial class LoginPage : ContentPage
{
    private readonly LoginViewModel _vm = new();
    private List<Usuario> _usuarios = [];

    public LoginPage()
    {
        InitializeComponent();
        _vm.NavegaAlPOS   += OnNavegaAlPOS;
        _vm.NavegaAlAdmin += OnNavegaAlAdmin;
        CargarUsuarios();
    }

    private void CargarUsuarios()
    {
        try
        {
            _usuarios = UsuarioDao.ObtenerActivos();
            pickerUsuario.ItemsSource = _usuarios.Select(u => u.Nombre).ToList();
        }
        catch (Exception ex)
        {
            MostrarError("Sin conexión: " + ex.Message);
        }
    }

    private void OnUsuarioChanged(object? sender, EventArgs e)
    {
        if (pickerUsuario.SelectedIndex < 0) return;
        var usuario = _usuarios[pickerUsuario.SelectedIndex];
        stackFondo.IsVisible = !usuario.EsAdmin;
    }

    private void OnEntryCompleted(object? sender, EventArgs e) => Intentar();
    private void OnAbrirTurnoClicked(object? sender, EventArgs e) => Intentar();

    private void Intentar()
    {
        OcultarError();
        if (pickerUsuario.SelectedIndex < 0)
        {
            MostrarError("Seleccione un usuario."); return;
        }

        var usuario = _usuarios[pickerUsuario.SelectedIndex];
        string pin  = entryPin.Text ?? "";

        if (string.IsNullOrWhiteSpace(pin))
        {
            MostrarError("Ingrese su PIN."); return;
        }

        if (!usuario.EsAdmin && string.IsNullOrWhiteSpace(entryFondo.Text))
        {
            MostrarError("Ingrese el fondo inicial."); return;
        }

        var verificado = UsuarioDao.VerificarPin(usuario.Nombre, pin);
        if (verificado == null) { MostrarError("PIN incorrecto."); return; }

        if (verificado.EsAdmin)
            OnNavegaAlAdmin(verificado);
        else
        {
            decimal fondo = decimal.TryParse(entryFondo.Text, out var f) ? f : 0;
            int turnoId   = VentaDao.AbrirTurno(verificado.Id, fondo);
            OnNavegaAlPOS(verificado, turnoId);
        }
    }

    private void OnNavegaAlPOS(Usuario usuario, int turnoId)
    {
        Application.Current!.Windows[0].Page = new POSPage(usuario, turnoId);
    }

    private void OnNavegaAlAdmin(Usuario usuario)
    {
        Application.Current!.Windows[0].Page = new AdminPage(usuario);
    }

    private void MostrarError(string msg)
    {
        lblError.Text = msg;
        lblError.IsVisible = true;
    }

    private void OcultarError() => lblError.IsVisible = false;
}
