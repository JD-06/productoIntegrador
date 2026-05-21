using Dapper;
using PosMaui.Modelo.Entidad;
using System.Security.Cryptography;
using System.Text;

namespace PosMaui.Modelo.Dao;

public static class UsuarioDao
{
    public static List<Usuario> ObtenerActivos()
    {
        using var conn = ConexionBD.Abrir();
        return conn.Query<Usuario>("""
            SELECT u.id AS Id, u.nombre AS Nombre, r.nombre AS Rol, u.activo AS Activo
            FROM usuarios u
            JOIN roles r ON r.id = u.rol_id
            WHERE u.activo = true
            ORDER BY u.nombre
            """).ToList();
    }

    public static Usuario? VerificarPin(string nombre, string pin)
    {
        string hash = Sha256(pin);
        using var conn = ConexionBD.Abrir();
        return conn.QueryFirstOrDefault<Usuario>("""
            SELECT u.id AS Id, u.nombre AS Nombre, r.nombre AS Rol, u.activo AS Activo
            FROM usuarios u
            JOIN roles r ON r.id = u.rol_id
            WHERE u.nombre = @Nombre AND u.pin_hash = @Hash AND u.activo = true
            """, new { Nombre = nombre, Hash = hash });
    }

    public static void Crear(string nombre, string pin, int rolId)
    {
        using var conn = ConexionBD.Abrir();
        conn.Execute("""
            INSERT INTO usuarios (nombre, pin_hash, rol_id)
            VALUES (@Nombre, @Hash, @RolId)
            """, new { Nombre = nombre, Hash = Sha256(pin), RolId = rolId });
    }

    public static void ToggleActivo(int id, bool activo)
    {
        using var conn = ConexionBD.Abrir();
        conn.Execute("UPDATE usuarios SET activo = @Activo WHERE id = @Id",
                     new { Activo = activo, Id = id });
    }

    public static string Sha256(string input)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(input));
        return Convert.ToHexString(bytes).ToLower();
    }
}
