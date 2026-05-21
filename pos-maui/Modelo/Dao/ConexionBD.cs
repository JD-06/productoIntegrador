using Npgsql;
using System.Data;

namespace PosMaui.Modelo.Dao;

/// <summary>
/// Administra la conexión a PostgreSQL.
/// Lee credenciales desde el archivo .env buscando hacia arriba en el árbol de directorios.
/// </summary>
public static class ConexionBD
{
    private static string? _connectionString;

    public static IDbConnection Abrir()
    {
        _connectionString ??= CargarConnectionString();
        var conn = new NpgsqlConnection(_connectionString);
        conn.Open();
        return conn;
    }

    private static string CargarConnectionString()
    {
        var props = LeerDotEnv();
        string url      = props.GetValueOrDefault("DB_URL", "");
        string user     = props.GetValueOrDefault("DB_USER", "");
        string password = props.GetValueOrDefault("DB_PASSWORD", "");

        // Convierte jdbc:postgresql://host:port/db a formato Npgsql
        if (url.StartsWith("jdbc:postgresql://"))
            url = url.Replace("jdbc:postgresql://", "");

        // Quita parámetros como ?sslmode=disable
        int q = url.IndexOf('?');
        if (q >= 0) url = url[..q];

        // url ahora es: host:port/database
        var parts = url.Split('/');
        string hostPort = parts[0];
        string database = parts.Length > 1 ? parts[1] : "postgres";
        string host     = hostPort.Split(':')[0];
        string port     = hostPort.Contains(':') ? hostPort.Split(':')[1] : "5432";

        return $"Host={host};Port={port};Database={database};Username={user};Password={password};SSL Mode=Disable";
    }

    private static Dictionary<string, string> LeerDotEnv()
    {
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        for (int i = 0; i < 6; i++)
        {
            var envFile = Path.Combine(dir.FullName, ".env");
            if (File.Exists(envFile))
                return ParsearEnv(envFile);
            if (dir.Parent == null) break;
            dir = dir.Parent;
        }
        return [];
    }

    private static Dictionary<string, string> ParsearEnv(string path)
    {
        var dict = new Dictionary<string, string>();
        foreach (var line in File.ReadAllLines(path))
        {
            var l = line.Trim();
            if (l.StartsWith('#') || !l.Contains('=')) continue;
            int eq = l.IndexOf('=');
            dict[l[..eq].Trim()] = l[(eq + 1)..].Trim();
        }
        return dict;
    }
}
