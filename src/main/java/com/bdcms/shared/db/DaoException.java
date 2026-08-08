package com.bdcms.shared.db;

/**
 * Unchecked wrapper for {@link java.sql.SQLException}.
 *
 * <p>DAOs throw this so that services and servlets are not forced to declare
 * checked SQL exceptions, while {@link Tx} can still detect a failure and roll
 * the transaction back.
 */
public class DaoException extends RuntimeException {

    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }

    public DaoException(String message) {
        super(message);
    }
}
