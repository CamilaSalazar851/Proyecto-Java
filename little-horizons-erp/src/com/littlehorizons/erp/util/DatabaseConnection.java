package com.littlehorizons.erp.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton that holds the database settings and hands out connections.
 * Credentials are read from config/db.properties so they never live in the code.
 */
public final class DatabaseConnection {

    private static final Path CONFIG_FILE = Path.of("config", "db.properties");
    private static DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection() {
        Properties props = new Properties();
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                props.load(in);
            } catch (IOException e) {
                AppLogger.error("Could not read " + CONFIG_FILE, e);
                throw new AppException("No se pudo leer el archivo de configuracion de la base de datos.", e);
            }
        }
        this.url = props.getProperty("db.url",
                "jdbc:mysql://localhost:3306/erp_little_horizons?characterEncoding=UTF-8&useSSL=false&serverTimezone=America/Bogota&allowPublicKeyRetrieval=true");
        this.user = props.getProperty("db.user", "root");
        this.password = props.getProperty("db.password", "");
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /** Returns a new connection. The caller must close it (try-with-resources). */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
