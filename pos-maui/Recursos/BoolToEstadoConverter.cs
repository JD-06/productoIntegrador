using System.Globalization;

namespace PosMaui.Recursos;

public class BoolToEstadoConverter : IValueConverter
{
    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
        => value is true ? "ACTIVO" : "INACTIVO";

    public object ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
        => value?.ToString() == "ACTIVO";
}
