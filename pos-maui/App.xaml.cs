using PosMaui.Vista.Login;

namespace PosMaui;

public partial class App : Application
{
    public App()
    {
        InitializeComponent();
    }

    protected override Window CreateWindow(IActivationState? activationState)
        => new Window(new LoginPage()) { Title = "POS Empresarial ERP" };
}
