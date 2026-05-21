package com.empresa.pos.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Base class for all DAOs.
 * Provides a DataSource and helper methods for connection management.
 */
public abstract class BaseDAO {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final DataSource dataSource;

    protected BaseDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Obtains a connection from the pool. Callers must close it (try-with-resources). */
    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Silently close a ResultSet, ignoring any exception. */
    protected void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException ignored) {}
        }
    }

    /** Silently close a PreparedStatement, ignoring any exception. */
    protected void closeQuietly(PreparedStatement ps) {
        if (ps != null) {
            try { ps.close(); } catch (SQLException ignored) {}
        }
    }
}
