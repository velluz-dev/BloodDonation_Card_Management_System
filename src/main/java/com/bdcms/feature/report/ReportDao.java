package com.bdcms.feature.report;

import com.bdcms.shared.db.DaoException;
import com.bdcms.shared.util.Dates;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only reporting queries (FR-7).
 *
 * <p>The one place in the system where SQL spans several features' tables.
 * That is a considered exception, not an oversight: a report is by nature a
 * cross-cutting read, and answering "donations per blood group" by loading
 * every donor into Java and grouping them there would be far slower and no
 * cleaner. The constraint that keeps it honest is that this DAO only ever
 * reads - no report can change anything another feature owns.
 */
public class ReportDao {

    /** Every donation in a period, newest first. */
    public List<ReportRow> donationsBetween(Connection conn, LocalDate from, LocalDate to) {
        String sql = "SELECT dn.donation_date, d.full_name, d.blood_group, c.card_no, "
                + "dn.centre, dn.volume_ml, u.full_name AS staff_name "
                + "FROM donations dn "
                + "JOIN donors d      ON d.donor_id = dn.donor_id "
                + "JOIN donor_cards c ON c.card_id  = dn.card_id "
                + "JOIN users u       ON u.user_id  = dn.verified_by "
                + "WHERE dn.donation_date BETWEEN ? AND ? "
                + "ORDER BY dn.donation_date DESC, dn.donation_id DESC";

        List<ReportRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Dates.toSqlDate(from));
            ps.setDate(2, Dates.toSqlDate(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(ReportRow.of(
                            String.valueOf(rs.getDate("donation_date").toLocalDate()),
                            rs.getString("full_name"),
                            label(rs.getString("blood_group")),
                            rs.getString("card_no"),
                            rs.getString("centre"),
                            rs.getInt("volume_ml") + " ml",
                            rs.getString("staff_name")));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DaoException("Donations report failed", e);
        }
    }

    /**
     * Donors and donations per blood group.
     *
     * <p>The donation counts come from a correlated subquery rather than a join,
     * so that a blood group with registered donors but no donations still
     * appears with a zero - a join would silently drop it, and "which groups are
     * we not collecting?" is exactly what this report is asked.
     */
    public List<ReportRow> bloodGroupDistribution(Connection conn) {
        String sql = "SELECT d.blood_group, COUNT(*) AS donor_count, "
                + "(SELECT COUNT(*) FROM donations dn "
                + "   JOIN donors d2 ON d2.donor_id = dn.donor_id "
                + "  WHERE d2.blood_group = d.blood_group) AS donation_count, "
                + "(SELECT COALESCE(SUM(dn.volume_ml), 0) FROM donations dn "
                + "   JOIN donors d2 ON d2.donor_id = dn.donor_id "
                + "  WHERE d2.blood_group = d.blood_group) AS total_ml "
                + "FROM donors d GROUP BY d.blood_group ORDER BY donor_count DESC";

        List<ReportRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(ReportRow.of(
                        label(rs.getString("blood_group")),
                        String.valueOf(rs.getInt("donor_count")),
                        String.valueOf(rs.getInt("donation_count")),
                        rs.getInt("total_ml") + " ml"));
            }
            return rows;
        } catch (SQLException e) {
            throw new DaoException("Blood group report failed", e);
        }
    }

    /**
     * Per-donor activity, including days since the last donation.
     *
     * <p>The eligibility verdict is not computed in SQL. That rule lives in
     * {@code EligibilityService} and is configurable at runtime; duplicating it
     * as a hard-coded 84 in a query would give two sources of truth that could
     * disagree. The query returns the facts and the service decides.
     */
    public List<DonorActivityRow> donorActivity(Connection conn) {
        String sql = "SELECT d.donor_id, d.full_name, d.blood_group, "
                + "COUNT(dn.donation_id) AS donation_count, "
                + "MAX(dn.donation_date) AS last_donation, "
                + "(SELECT c.card_no FROM donor_cards c "
                + "  WHERE c.donor_id = d.donor_id AND c.status = 'ACTIVE' "
                + "  ORDER BY c.issued_on DESC LIMIT 1) AS active_card "
                + "FROM donors d LEFT JOIN donations dn ON dn.donor_id = d.donor_id "
                + "GROUP BY d.donor_id, d.full_name, d.blood_group "
                + "ORDER BY donation_count DESC, d.full_name";

        List<DonorActivityRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new DonorActivityRow(
                        rs.getLong("donor_id"),
                        rs.getString("full_name"),
                        label(rs.getString("blood_group")),
                        rs.getInt("donation_count"),
                        Dates.date(rs, "last_donation"),
                        rs.getString("active_card")));
            }
            return rows;
        } catch (SQLException e) {
            throw new DaoException("Donor activity report failed", e);
        }
    }

    /** Raw facts about one donor's activity, before the eligibility rule is applied. */
    public record DonorActivityRow(long donorId, String fullName, String bloodGroup,
                                   int donationCount, LocalDate lastDonation, String activeCardNo) {
    }

    /** Turns the stored {@code A_POS} form into the printable {@code A+}. */
    private String label(String stored) {
        return com.bdcms.feature.donor.BloodGroup.parse(stored)
                .map(com.bdcms.feature.donor.BloodGroup::getLabel)
                .orElse(stored == null ? "-" : stored);
    }
}
