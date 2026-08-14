package com.bdcms.feature.verification;

import com.bdcms.shared.db.DaoException;
import com.bdcms.shared.util.Dates;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All SQL against the {@code verification_log} table (FR-4). Insert and read only - never update. */
public class VerificationLogDao {

    public long insert(Connection conn, VerificationLog log) {
        String sql = "INSERT INTO verification_log (card_no, donor_id, staff_id, result, detail) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, log.getCardNo());
            if (log.getDonorId() == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, log.getDonorId());
            }
            ps.setLong(3, log.getStaffId());
            ps.setString(4, log.getResult().name());
            ps.setString(5, log.getDetail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                log.setLogId(id);
                return id;
            }
        } catch (SQLException e) {
            throw new DaoException("Could not write verification log entry", e);
        }
    }

    /**
     * The audit trail, newest first, optionally narrowed by donor and date.
     *
     * <p>Joins users and donors for display. This DAO reads those tables but
     * only to resolve names for the audit screen - it never writes to them, and
     * the donor and auth features remain the owners of their own data.
     */
    public List<VerificationLog> search(Connection conn, Long donorId, LocalDate from, LocalDate to,
                                        int limit, int offset) {
        StringBuilder sql = new StringBuilder(
                "SELECT v.log_id, v.card_no, v.donor_id, v.staff_id, v.result, v.detail, "
                        + "v.attempted_at, d.full_name AS donor_name, u.full_name AS staff_name "
                        + "FROM verification_log v "
                        + "LEFT JOIN donors d ON d.donor_id = v.donor_id "
                        + "JOIN users u ON u.user_id = v.staff_id "
                        + "WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (donorId != null) {
            sql.append(" AND v.donor_id = ?");
            params.add(donorId);
        }
        if (from != null) {
            sql.append(" AND v.attempted_at >= ?");
            params.add(java.sql.Timestamp.valueOf(from.atStartOfDay()));
        }
        if (to != null) {
            sql.append(" AND v.attempted_at < ?");
            params.add(java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
        }
        sql.append(" ORDER BY v.attempted_at DESC, v.log_id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<VerificationLog> entries = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(map(rs));
                }
            }
            return entries;
        } catch (SQLException e) {
            throw new DaoException("Could not read the verification log", e);
        }
    }

    /**
     * How many attempts ended in each outcome over a period.
     *
     * <p>Every {@link VerificationResult} appears, including the ones with no
     * occurrences: "zero photo mismatches this month" is a finding, and a row
     * that silently vanishes cannot report it.
     */
    public Map<VerificationResult, Integer> countByResult(Connection conn, LocalDate from, LocalDate to) {
        Map<VerificationResult, Integer> counts = new LinkedHashMap<>();
        for (VerificationResult result : VerificationResult.values()) {
            counts.put(result, 0);
        }

        String sql = "SELECT result, COUNT(*) AS n FROM verification_log "
                + "WHERE attempted_at >= ? AND attempted_at < ? GROUP BY result";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(VerificationResult.valueOf(rs.getString("result")), rs.getInt("n"));
                }
            }
            return counts;
        } catch (SQLException e) {
            throw new DaoException("Could not summarise verification outcomes", e);
        }
    }

    public int countSince(Connection conn, LocalDate from, boolean failuresOnly) {
        String sql = "SELECT COUNT(*) FROM verification_log WHERE attempted_at >= ?"
                + (failuresOnly ? " AND result <> 'SUCCESS'" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(from.atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Could not count verification attempts", e);
        }
    }

    private VerificationLog map(ResultSet rs) throws SQLException {
        VerificationLog log = new VerificationLog();
        log.setLogId(rs.getLong("log_id"));
        log.setCardNo(rs.getString("card_no"));
        long donorId = rs.getLong("donor_id");
        log.setDonorId(rs.wasNull() ? null : donorId);
        log.setStaffId(rs.getLong("staff_id"));
        log.setResult(VerificationResult.valueOf(rs.getString("result")));
        log.setDetail(rs.getString("detail"));
        log.setAttemptedAt(Dates.dateTime(rs, "attempted_at"));
        log.setDonorName(rs.getString("donor_name"));
        log.setStaffName(rs.getString("staff_name"));
        return log;
    }
}
