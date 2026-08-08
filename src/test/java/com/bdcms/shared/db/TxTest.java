package com.bdcms.shared.db;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the transaction boundary (NFR Reliability).
 *
 * <p>This is the contract the whole donation flow rests on: work that returns
 * normally is committed, work that throws anything at all is rolled back, and
 * the connection goes back to the pool either way. Getting it wrong would mean
 * a half-recorded donation, so it is asserted rather than assumed.
 *
 * <p>The fake {@link Connection} is a dynamic proxy that simply records which
 * methods were called. Implementing the interface by hand would mean forty
 * stub methods that say nothing about the behaviour under test - and would
 * break whenever JDBC adds one.
 */
class TxTest {

    @AfterEach
    void clearInjectedPool() throws Exception {
        setPool(null);
    }

    @Test
    @DisplayName("work that returns normally is committed")
    void commitsOnSuccess() throws Exception {
        RecordingConnection conn = inject();

        String result = Tx.inTransaction(c -> "done");

        assertEquals("done", result);
        assertTrue(conn.called("commit"), "expected a commit");
        assertFalse(conn.called("rollback"), "must not roll back a successful unit of work");
        assertTrue(conn.called("close"), "connection must be returned to the pool");
    }

    @Test
    @DisplayName("autoCommit is switched off, so the statements form one unit")
    void disablesAutoCommit() throws Exception {
        RecordingConnection conn = inject();

        Tx.inTransaction(c -> null);

        assertTrue(conn.called("setAutoCommit"),
                "without this every statement would commit itself and nothing could roll back");
    }

    @Test
    @DisplayName("a SQLException rolls the transaction back and surfaces as DaoException")
    void rollsBackOnSqlException() throws Exception {
        RecordingConnection conn = inject();

        assertThrows(DaoException.class, () -> Tx.inTransaction(c -> {
            throw new SQLException("insert failed");
        }));

        assertTrue(conn.called("rollback"), "expected a rollback");
        assertFalse(conn.called("commit"), "nothing may be committed after a failure");
        assertTrue(conn.called("close"));
    }

    @Test
    @DisplayName("a RuntimeException also rolls back, and is rethrown unchanged")
    void rollsBackOnRuntimeException() throws Exception {
        RecordingConnection conn = inject();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> Tx.inTransaction(c -> {
                    throw new IllegalStateException("business rule violated");
                }));

        // A DaoException thrown by a DAO inside the block must not be swallowed
        // or rewrapped - the caller needs the original failure.
        assertEquals("business rule violated", thrown.getMessage());
        assertTrue(conn.called("rollback"));
        assertFalse(conn.called("commit"));
    }

    @Test
    @DisplayName("read() runs the work and closes the connection without a transaction")
    void readDoesNotOpenATransaction() throws Exception {
        RecordingConnection conn = inject();

        assertEquals("ok", Tx.read(c -> "ok"));

        assertTrue(conn.called("close"));
        assertFalse(conn.called("commit"), "a read-only query needs no commit");
    }

    @Test
    @DisplayName("read() wraps a SQLException as DaoException")
    void readWrapsSqlException() throws Exception {
        RecordingConnection conn = inject();

        DaoException thrown = assertThrows(DaoException.class,
                () -> Tx.read(c -> {
                    throw new SQLException("bad query");
                }));

        assertEquals("Query failed", thrown.getMessage());
        assertTrue(conn.called("close"));
    }

    @Test
    @DisplayName("using Tx before the pool is built fails loudly rather than silently")
    void failsFastWithoutAPool() throws Exception {
        setPool(null);

        assertThrows(IllegalStateException.class, () -> Tx.read(c -> "never runs"));
    }

    // ---- test doubles ------------------------------------------------------

    /** Installs a recording connection behind {@link Datasource} and returns it. */
    private RecordingConnection inject() throws Exception {
        RecordingConnection recording = new RecordingConnection();
        setPool(new StubDataSource(recording.proxy()));
        return recording;
    }

    private static void setPool(HikariDataSource dataSource) throws Exception {
        Field field = Datasource.class.getDeclaredField("pool");
        field.setAccessible(true);
        field.set(null, dataSource);
    }

    /** A HikariDataSource that hands out one fixed connection. */
    private static final class StubDataSource extends HikariDataSource {
        private final Connection connection;

        private StubDataSource(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Connection getConnection() {
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) {
            return connection;
        }
    }

    /** Records every method invoked on the connection it proxies. */
    private static final class RecordingConnection {
        private final List<String> calls = new ArrayList<>();

        private Connection proxy() {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        calls.add(method.getName());
                        return defaultValueFor(method.getReturnType());
                    });
        }

        private boolean called(String methodName) {
            return calls.contains(methodName);
        }

        private static Object defaultValueFor(Class<?> type) {
            if (!type.isPrimitive()) {
                return null;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == void.class) {
                return null;
            }
            return 0;
        }
    }
}
