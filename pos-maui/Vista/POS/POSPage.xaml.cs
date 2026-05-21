using PosMaui.Controlador.POS;
using PosMaui.Modelo.Dao;
using PosMaui.Modelo.Entidad;
using PosMaui.Modelo.Servicio;
using PosMaui.Vista.Login;

namespace PosMaui.Vista.POS;

public partial class POSPage : ContentPage
{
    private readonly POSViewModel _vm;
    private readonly Usuario      _cajero;
    private readonly int          _turnoId;

    public POSPage(Usuario cajero, int turnoId)
    {
        InitializeComponent();
        _cajero  = cajero;
        _turnoId = turnoId;
        _vm      = new POSViewModel();
        _vm.AbrirCobro += MostrarCobro;
        _vm.Inicializar(cajero.Id, cajero.Nombre, turnoId);

        lblCajero.Text = cajero.Nombre;
        lblTurno.Text  = $"Turno #{turnoId}";

        ActualizarVista();
        RenderizarCatalogo(_vm.CatalogoVisible);
    }

    private void OnSkuIngresado(object? sender, EventArgs e)
    {
        string sku = entrySku.Text?.Trim() ?? "";
        if (string.IsNullOrEmpty(sku)) return;
        var producto = _vm.CatalogoVisible.FirstOrDefault(p =>
            p.Sku.Equals(sku, StringComparison.OrdinalIgnoreCase));
        if (producto != null) AgregarProducto(producto);
        else lblEstado.Text = $"SKU no encontrado: {sku}";
        entrySku.Text = "";
    }

    private void OnBusquedaChanged(object? sender, TextChangedEventArgs e)
    {
        _vm.Busqueda = e.NewTextValue ?? "";
        RenderizarCatalogo(_vm.CatalogoVisible);
    }

    private void RenderizarCatalogo(List<Producto> productos)
    {
        flexProductos.Children.Clear();
        foreach (var p in productos)
        {
            var card = CrearTarjetaProducto(p);
            flexProductos.Children.Add(card);
        }
    }

    private View CrearTarjetaProducto(Producto producto)
    {
        var border = new Border
        {
            BackgroundColor = Colors.White,
            StrokeThickness = 0,
            WidthRequest    = 130,
            HeightRequest   = 110,
            Margin          = new Thickness(4),
            Padding         = new Thickness(8),
        };
        border.StrokeShape = new Microsoft.Maui.Controls.Shapes.RoundRectangle
            { CornerRadius = new CornerRadius(6) };

        var stack = new VerticalStackLayout { Spacing = 4, HorizontalOptions = LayoutOptions.Center };

        if (producto.StockActual <= 0)
            stack.Add(new Label { Text = "SIN STOCK", TextColor = Colors.Red,
                                  FontSize = 9, HorizontalOptions = LayoutOptions.Center });

        stack.Add(new Label
        {
            Text              = producto.Nombre,
            FontSize          = 11,
            LineBreakMode     = LineBreakMode.WordWrap,
            MaxLines          = 2,
            HorizontalOptions = LayoutOptions.Center,
            HorizontalTextAlignment = TextAlignment.Center
        });
        stack.Add(new Label
        {
            Text              = producto.PrecioFormateado,
            FontSize          = 13,
            FontAttributes    = FontAttributes.Bold,
            TextColor         = Color.FromArgb("#1A2D5A"),
            HorizontalOptions = LayoutOptions.Center
        });
        stack.Add(new Label
        {
            Text              = $"Stock: {producto.StockActual}",
            FontSize          = 9,
            TextColor         = producto.BajoStock ? Colors.Red : Colors.Gray,
            HorizontalOptions = LayoutOptions.Center
        });

        border.Content = stack;

        var tap = new TapGestureRecognizer();
        tap.Tapped += (_, _) => AgregarProducto(producto);
        border.GestureRecognizers.Add(tap);

        return border;
    }

    private void AgregarProducto(Producto p)
    {
        _vm.AgregarAlCarritoCommand.Execute(p);
        ActualizarVista();
        lblEstado.Text = _vm.Estado;
    }

    private void ActualizarVista()
    {
        listaCarrito.ItemsSource = null;
        listaCarrito.ItemsSource = _vm.Carrito;
        lblSubtotal.Text = $"${_vm.Subtotal:F2}";
        lblIva.Text      = $"${_vm.Iva:F2}";
        lblTotal.Text    = $"${_vm.Total:F2}";
        lblItems.Text    = $"{_vm.Carrito.Count} items";
    }

    private void OnEliminarItemClicked(object? sender, EventArgs e)
    {
        if (sender is Button btn && btn.CommandParameter is CartItem item)
        {
            _vm.EliminarItemCommand.Execute(item);
            ActualizarVista();
        }
    }

    private void OnQuitarClicked(object? sender, EventArgs e)
    {
        var sel = listaCarrito.SelectedItem as CartItem;
        if (sel != null) { _vm.EliminarItemCommand.Execute(sel); ActualizarVista(); }
    }

    private void OnCancelarVentaClicked(object? sender, EventArgs e)
    {
        _vm.CancelarVentaCommand.Execute(null);
        ActualizarVista();
    }

    private void OnCajonClicked(object? sender, EventArgs e)
        => lblEstado.Text = "[F4] Cajón abierto";

    private async void OnCobrarClicked(object? sender, EventArgs e)
    {
        await _vm.CobrarCommand.ExecuteAsync(null);
        ActualizarVista();
    }

    private async Task MostrarCobro(CartItem[] items, decimal subtotal, decimal iva,
                                     decimal total, int cajeroId, int turnoId)
    {
        string metodo = await DisplayActionSheet("Método de pago", "Cancelar", null,
                                                  "EFECTIVO", "TARJETA") ?? "Cancelar";
        if (metodo == "Cancelar") return;

        decimal montoRecibido = total;
        decimal cambio = 0;

        if (metodo == "EFECTIVO")
        {
            string? montoStr = await DisplayPromptAsync("Monto recibido",
                $"Total: ${total:F2}\nIngrese monto recibido:",
                initialValue: total.ToString("F2"), keyboard: Keyboard.Numeric);
            if (montoStr == null) return;
            if (!decimal.TryParse(montoStr, out montoRecibido) || montoRecibido < total)
            {
                await DisplayAlert("Error", "Monto insuficiente.", "OK"); return;
            }
            cambio = montoRecibido - total;
        }

        int metodoPagoId = metodo == "EFECTIVO" ? 1 : 2;

        int ventaId = VentaDao.GuardarVenta(turnoId, cajeroId, subtotal, iva, total,
                                             metodoPagoId, montoRecibido, cambio, [.. items]);

        if (metodo == "EFECTIVO")
            await DisplayAlert("Cambio", $"Cambio: ${cambio:F2}", "OK");

        // Guardar ticket TXT
        string txt = TicketService.GenerarTxt(ventaId, items, subtotal, iva, total,
                                               _cajero.Nombre, metodo, montoRecibido, cambio);
        string docPath = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
        string nombre  = $"ticket_{ventaId}_{DateTime.Now:yyyyMMdd_HHmmss}";
        await File.WriteAllTextAsync(Path.Combine(docPath, nombre + ".txt"), txt);
        await File.WriteAllTextAsync(Path.Combine(docPath, nombre + ".html"),
            TicketService.GenerarHtml(ventaId, items, subtotal, iva, total,
                                      _cajero.Nombre, metodo));

        await DisplayAlert("Venta completada",
            $"Venta #{ventaId} guardada.\nTicket en Documentos/{nombre}.txt", "OK");

        _vm.LimpiarCarrito();
        ActualizarVista();
    }

    private void OnCerrarTurnoClicked(object? sender, EventArgs e)
        => Application.Current!.Windows[0].Page = new LoginPage();
}
