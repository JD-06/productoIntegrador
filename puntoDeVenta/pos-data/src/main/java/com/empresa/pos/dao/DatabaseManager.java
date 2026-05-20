package com.empresa.pos.dao;

import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.Properties;

public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private final DataSource dataSource;

    public DatabaseManager() {
        Properties props = cargarConfiguracion();

        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(props.getProperty("db.url"));
        ds.setUser(props.getProperty("db.user"));
        ds.setPassword(props.getProperty("db.password"));

        this.dataSource = ds;
        log.info("Conectando a: {}", props.getProperty("db.url"));

        AppContext.init(this.dataSource,
                props.getProperty("images.path"),
                props.getProperty("json.path"));
    }

    public void initializeDatabase() {
        verificarConexion();

        Flyway flyway = Flyway.configure()
                .dataSource(this.dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        int migraciones = flyway.migrate().migrationsExecuted;
        log.info("Flyway: {} migracion(es) ejecutada(s).", migraciones);
    }

    public DataSource getDataSource() { return dataSource; }

    public Connection getConnection() throws Exception { return dataSource.getConnection(); }

    private void verificarConexion() {
        try (Connection conn = dataSource.getConnection()) {
            log.info("Conexion a PostgreSQL exitosa. Version: {}",
                    conn.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            throw new RuntimeException(
                "No se pudo conectar a la base de datos. " +
                "Verifica el archivo .env y que el puerto 5432 este accesible.", e);
        }
    }

    /**
     * Carga configuracion desde .env (prioritario) o database.properties (fallback).
     * Busca .env en el directorio de trabajo y en sus directorios padre.
     */
    private Properties cargarConfiguracion() {
        Properties props = cargarDotEnv();
        if (props != null) {
            log.info("Configuracion cargada desde .env");
            return props;
        }

        log.info("Archivo .env no encontrado, usando database.properties");
        return cargarPropertiesFile();
    }

    private Properties cargarDotEnv() {
        Path dir = Paths.get(System.getProperty("user.dir"));
        for (int i = 0; i < 4; i++) {
            Path envFile = dir.resolve(".env");
            if (Files.exists(envFile)) {
                try {
                    Properties props = parsearDotEnv(envFile);
                    log.debug("Archivo .env encontrado en: {}", envFile);
                    return props;
                } catch (IOException e) {
                    log.warn("Error leyendo .env: {}", e.getMessage());
                }
            }
            if (dir.getParent() == null) break;
            dir = dir.getParent();
        }
        return null;
    }

    private Properties parsearDotEnv(Path envFile) throws IOException {
        Properties props = new Properties();
        for (String line : Files.readAllLines(envFile)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).trim();
            String val = line.substring(eq + 1).trim();
            switch (key) {
                case "DB_URL"       -> props.setProperty("db.url",       val);
                case "DB_USER"      -> props.setProperty("db.user",      val);
                case "DB_PASSWORD"  -> props.setProperty("db.password",  val);
                case "IMAGES_PATH"  -> props.setProperty("images.path",  val);
                case "JSON_PATH"    -> props.setProperty("json.path",    val);
            }
        }
        return props;
    }

    private Properties cargarPropertiesFile() {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/database.properties")) {
            if (in == null) throw new RuntimeException(
                "No se encontro ni .env ni database.properties. " +
                "Crea pos-ui/.env con DB_URL, DB_USER y DB_PASSWORD.");
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Error al leer database.properties", e);
        }
        return props;
    }
}
