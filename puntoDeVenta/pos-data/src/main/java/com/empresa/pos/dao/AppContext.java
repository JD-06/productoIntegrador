package com.empresa.pos.dao;

import javax.sql.DataSource;

/**
 * Application-wide singleton context.
 * Holds the DataSource and path configuration, and provides DAO factory methods.
 */
public final class AppContext {

    private static AppContext instance;

    private final DataSource dataSource;
    private final String imagesPath;
    private final String jsonPath;

    private AppContext(DataSource dataSource, String imagesPath, String jsonPath) {
        this.dataSource = dataSource;
        this.imagesPath = imagesPath;
        this.jsonPath   = jsonPath;
    }

    /**
     * Must be called once at application startup, before any DAO is used.
     */
    public static synchronized void init(DataSource dataSource, String imagesPath, String jsonPath) {
        if (instance != null) {
            throw new IllegalStateException("AppContext ya fue inicializado.");
        }
        instance = new AppContext(dataSource, imagesPath, jsonPath);
    }

    public static synchronized AppContext getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "AppContext no ha sido inicializado. Llama AppContext.init() primero.");
        }
        return instance;
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------

    public DataSource getDataSource() { return dataSource; }
    public String getImagesPath()     { return imagesPath; }
    public String getJsonPath()       { return jsonPath; }

    // ---------------------------------------------------------------
    // DAO factory methods
    // ---------------------------------------------------------------

    public ProductoDAO productoDAO()       { return new ProductoDAO(dataSource); }
    public CategoriaDAO categoriaDAO()     { return new CategoriaDAO(dataSource); }
    public ClienteDAO clienteDAO()         { return new ClienteDAO(dataSource); }
    public TurnoDAO turnoDAO()             { return new TurnoDAO(dataSource); }
    public VentaDAO ventaDAO()             { return new VentaDAO(dataSource); }
    public InventarioDAO inventarioDAO()   { return new InventarioDAO(dataSource); }
    public CompraDAO compraDAO()           { return new CompraDAO(dataSource); }
    public CuentaDAO cuentaDAO()           { return new CuentaDAO(dataSource); }
    public EmpleadoDAO empleadoDAO()       { return new EmpleadoDAO(dataSource); }
    public NominaDAO nominaDAO()           { return new NominaDAO(dataSource); }
    public LogAuditoriaDAO logDAO()        { return new LogAuditoriaDAO(dataSource); }
    public UsuarioDAO usuarioDAO()         { return new UsuarioDAO(dataSource); }
}
