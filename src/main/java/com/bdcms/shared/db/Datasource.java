package com.bdcms.shared.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Owns the single application-wide HikariCP connection pool.
 *
 * <p>Configured from {@code db.properties} on the classpath. System properties
 * of the same name override the file, which lets the pool be pointed at a
 * different database without rebuilding the WAR:
 *
 * <pre>-Dbdcms.db.url=jdbc:mysql://otherhost:3306/bdcms</pre>
 *
 * <p>This is the only class in the system that knows how to obtain a
 * {@link java.sql.Connection}. Features never construct their own.
 */
public final class Datasource {

    private static final String CONFIG_FILE = "db.properties";

    private static volatile HikariDataSource pool;

    private Datasource() {
    }

    /** Builds the pool. Called once by {@code AppLifecycleListener} at startup. */
    public static synchronized void init() {
        if (pool != null) {
            return;
        }
        Properties props = load();

        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("bdcms-pool");
        cfg.setJdbcUrl(get(props, "bdcms.db.url"));
        cfg.setUsername(get(props, "bdcms.db.username"));
        cfg.setPassword(get(props, "bdcms.db.password"));
        cfg.setDriverClassName(props.getProperty("bdcms.db.driver", "com.mysql.cj.jdbc.Driver"));
        cfg.setMaximumPoolSize(Integer.parseInt(props.getProperty("bdcms.db.pool.max", "10")));
        cfg.setMinimumIdle(Integer.parseInt(props.getProperty("bdcms.db.pool.minIdle", "2")));
        cfg.setConnectionTimeout(Long.parseLong(props.getProperty("bdcms.db.pool.timeoutMs", "10000")));
        // Donor lookup must return within 2s (NFR: Performance) - fail fast rather than queue.
        cfg.setValidationTimeout(3000);
        cfg.setAutoCommit(true);

        pool = new HikariDataSource(cfg);
    }

    /** The shared pool. */
    public static DataSource get() {
        HikariDataSource p = pool;
        if (p == null) {
            throw new IllegalStateException(
                    "Datasource has not been initialised - AppLifecycleListener did not run.");
        }
        return p;
    }

    /** Closes the pool. Called by {@code AppLifecycleListener} at shutdown. */
    public static synchronized void close() {
        if (pool != null) {
            pool.close();
            pool = null;
        }
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = Datasource.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException(CONFIG_FILE + " not found on the classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + CONFIG_FILE, e);
        }
        return props;
    }

    /** System property wins over the file, so deployments can be reconfigured without a rebuild. */
    private static String get(Properties props, String key) {
        String value = System.getProperty(key, props.getProperty(key));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required database setting: " + key);
        }
        return value;
    }
}
