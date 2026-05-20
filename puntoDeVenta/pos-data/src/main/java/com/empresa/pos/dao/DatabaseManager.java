package com.empresa.pos.dao;

import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.File;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:pos_local.db";
    private final DataSource dataSource;

    public DatabaseManager() {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl(DB_URL);
        this.dataSource = ds;
    }

    public void initializeDatabase() {
        System.out.println("Initializing database at " + DB_URL);
        
        Flyway flyway = Flyway.configure()
                .dataSource(this.dataSource)
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();
        System.out.println("Database migration completed successfully.");
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
