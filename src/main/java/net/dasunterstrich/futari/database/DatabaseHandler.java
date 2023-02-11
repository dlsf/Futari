package net.dasunterstrich.futari.database;

import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseHandler {
    private HikariDataSource dataSource;

    public void initializeConnectionPool() {
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + Path.of("futari.sqlite").toAbsolutePath());
        dataSource.setMinimumIdle(2);
        try {
            dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closeDataSource() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
