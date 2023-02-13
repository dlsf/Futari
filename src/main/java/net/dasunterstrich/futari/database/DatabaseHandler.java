package net.dasunterstrich.futari.database;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private HikariDataSource dataSource;

    public void initializeDatabase() {
        initializeConnectionPool();
        initializeTables();
    }

    private void initializeConnectionPool() {
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + Path.of("futari.sqlite").toAbsolutePath());
        dataSource.setMinimumIdle(2);
        try {
            dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeTables() {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS ReportThreads (user_id INTEGER PRIMARY KEY, thread_id INTEGER)");
            statement.execute("CREATE TABLE IF NOT EXISTS Punishments (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, moderator_id INTEGER, type TEXT, reason TEXT, comment TEXT, timestamp INTEGER, duration TEXT)");
            statement.execute("CREATE TABLE IF NOT EXISTS TemporaryPunishments (report_id INTEGER PRIMARY KEY, timestamp INTEGER, done INTEGER DEFAULT 0, FOREIGN KEY(report_id) REFERENCES Punishments(id))");
        } catch (SQLException exception) {
            logger.error("Could not create tables", exception);
            System.exit(-1);
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
