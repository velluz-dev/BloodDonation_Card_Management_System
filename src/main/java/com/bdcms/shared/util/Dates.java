package com.bdcms.shared.util;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Conversions between {@code java.time} types used in the domain and the
 * {@code java.sql} types JDBC exposes, with null handling in one place.
 *
 * <p>Also carries the display formatters. JSTL's {@code fmt} tags predate
 * {@code java.time} and cannot format a {@link LocalDateTime}, so models expose
 * pre-formatted strings built here rather than making each JSP re-parse an ISO
 * timestamp - which would be both fragile and logic in a view.
 */
public final class Dates {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private Dates() {
    }

    /** Formats a timestamp for display, e.g. {@code 07 Aug 2026, 14:32}. */
    public static String display(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    /** Formats a date for display, e.g. {@code 07 Aug 2026}. */
    public static String display(LocalDate value) {
        return value == null ? "" : DATE.format(value);
    }

    public static LocalDate toLocalDate(java.sql.Date value) {
        return value == null ? null : value.toLocalDate();
    }

    public static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    public static Date toSqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    public static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    /** Reads a nullable DATE column. */
    public static LocalDate date(ResultSet rs, String column) throws SQLException {
        return toLocalDate(rs.getDate(column));
    }

    /** Reads a nullable DATETIME/TIMESTAMP column. */
    public static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        return toLocalDateTime(rs.getTimestamp(column));
    }
}
