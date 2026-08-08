package com.bdcms.shared.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Unit-of-work helper.
 *
 * <p>Every DAO method in this system takes a {@link Connection} as its first
 * argument. That is what makes the reliability requirement achievable: several
 * DAOs belonging to <em>different features</em> can enlist in one transaction,
 * because they are all handed the same connection.
 *
 * <p>Two entry points:
 * <ul>
 *   <li>{@link #inTransaction(Work)} - explicit commit/rollback boundary, used
 *       whenever anything is written (donation recording, card issue).</li>
 *   <li>{@link #read(Work)} - a single auto-commit connection for read-only
 *       work, so simple lookups do not pay for transaction bookkeeping.</li>
 * </ul>
 */
public final class Tx {

    /** A block of work that needs a database connection. */
    @FunctionalInterface
    public interface Work<T> {
        T apply(Connection conn) throws SQLException;
    }

    private Tx() {
    }

    /**
     * Runs {@code work} inside a single transaction: commits if it returns
     * normally, rolls back if it throws anything at all.
     */
    public static <T> T inTransaction(Work<T> work) {
        Connection conn = null;
        boolean previousAutoCommit = true;
        try {
            conn = Datasource.get().getConnection();
            previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            T result = work.apply(conn);

            conn.commit();
            return result;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DaoException("Transaction failed and was rolled back", e);
        } catch (RuntimeException e) {
            rollbackQuietly(conn);
            throw e;
        } finally {
            closeQuietly(conn, previousAutoCommit);
        }
    }

    /** Runs read-only {@code work} on an auto-commit connection. */
    public static <T> T read(Work<T> work) {
        try (Connection conn = Datasource.get().getConnection()) {
            return work.apply(conn);
        } catch (SQLException e) {
            throw new DaoException("Query failed", e);
        }
    }

    private static void rollbackQuietly(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException ignored) {
            // Nothing useful can be done; the original failure is the one that matters.
        }
    }

    private static void closeQuietly(Connection conn, boolean restoreAutoCommit) {
        if (conn == null) {
            return;
        }
        try {
            conn.setAutoCommit(restoreAutoCommit);
        } catch (SQLException ignored) {
            // Connection is being returned to the pool regardless.
        }
        try {
            conn.close();
        } catch (SQLException ignored) {
            // As above.
        }
    }
}
