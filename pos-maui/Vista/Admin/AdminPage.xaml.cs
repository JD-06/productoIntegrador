using PosMaui.Controlador.Admin;
using PosMaui.Modelo.Dao;
using PosMaui.Modelo.Entidad;
using PosMaui.Vista.Login;

namespace PosMaui.Vista.Admin;

public partial class AdminPage : ContentPage
{
    private readonly AdminViewModel _vm;

    public AdminPage(Usuario usuario)
    {
        InitializeComponent();
        _vm = new AdminViewModel();
        _vm.Inicializar(usuario.Nombre);
        lblAdmin.Text = $"Admin: {usuario.Nombre}";
        ActualizarVista();
    }

    private void ActualizarVista()
    {
        lblVentasGlobales.Text = $"${_vm.VentasGlobales:F2}";
        lblCortes.Text         = _vm.Cortes.ToString();
        lblTurnosActivos.Text  = _vm.TurnosActivos.ToString();
        listaAlertas.ItemsSource  = _vm.AlertasStock;
        listaCatalogo.ItemsSource = _vm.Catalogo;
        listaUsuarios.ItemsSource = _vm.Usuarios;

        _vm.CargarCatalogoCommand.Execute(null);
        _vm.CargarUsuariosCommand.Execute(null);
        listaCatalogo.ItemsSource = _vm.Catalogo;
        listaUsuarios.ItemsSource = _vm.Usuarios;
    }

    private void OnBusquedaCatalogoChanged(object? sender, TextChangedEventArgs e)
    {
        _vm.BusquedaCatalogo = e.NewTextValue ?? "";
        listaCatalogo.ItemsSource = null;
        listaCatalogo.ItemsSource = _vm.Catalogo;
    }

    private async void OnNuevoUsuarioClicked(object? sender, EventArgs e)
    {
        string? nombre = await DisplayPromptAsync("Nuevo Usuario", "Nombre del operador:");
        if (string.IsNullOrWhiteSpace(nombre)) return;

        string? pin = await DisplayPromptAsync("Nuevo Usuario", "PIN de acceso:");
        if (string.IsNullOrWhiteSpace(pin)) return;

        string? rol = await DisplayActionSheet("Rol", "Cancelar", null, "CAJERO", "SUPERVISOR", "ADMIN");
        if (rol == null || rol == "Cancelar") return;

        try
        {
            var roles = Dapper.SqlMapper.Query<(int Id, string Nombre)>(
                ConexionBD.Abrir(),
                "SELECT id AS Item1, nombre AS Item2 FROM roles WHERE nombre = @Rol",
                new { Rol = rol }).FirstOrDefault();

            UsuarioDao.Crear(nombre, pin, roles.Id);
            _vm.CargarUsuariosCommand.Execute(null);
            listaUsuarios.ItemsSource = null;
            listaUsuarios.ItemsSource = _vm.Usuarios;
            await DisplayAlert("OK", $"Usuario '{nombre}' creado.", "OK");
        }
        catch (Exception ex)
        {
            await DisplayAlert("Error", ex.Message, "OK");
        }
    }

    private void OnSalirClicked(object? sender, EventArgs e)
        => Application.Current!.Windows[0].Page = new LoginPage();
}
