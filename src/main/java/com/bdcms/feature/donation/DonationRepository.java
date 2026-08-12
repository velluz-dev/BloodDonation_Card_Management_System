package com.bdcms.feature.donation;

import com.bdcms.shared.db.DaoException;
import com.bdcms.shared.util.Dates;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Simple repository for FR-5/FR-6 donation data access. */
public class DonationRepository {

    private static final String SELECT_WITH_JOINS =
            "SELECT dn.donation_id, dn.donor_id, dn.card_id, dn.donation_date, dn.centre, "
                    + "dn.volume_ml, dn.verified_by, dn.recorded_at, "
                    + "d.full_name AS donor_name, d.national_id, d.blood_group, "
                    + "c.card_no, u.full_name AS verified_by_name "
                    + "FROM donations dn "
                    + "JOIN donors d      ON d.donor_id = dn.donor_id "
                    + "JOIN donor_cards c ON c.card_id  = dn.card_id "
                    + "JOIN users u       ON u.user_id  = dn.verified_by ";

    public Optional<LocalDate> findLastDonationDate(Connection conn, long donorId) {
        String sql = "SELECT donation_date FROM donations WHERE donor_id = ? "
                + "ORDER BY donation_date DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(Dates.date(rs, "donation_date")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not read the last donation date for donor " + donorId, e);
        }
    }

    public long insert(Connection conn, Donation donation) {
        String sql = "INSERT INTO donations (donor_id, card_id, donation_date, centre, volume_ml, verified_by) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, donation.getDonorId());
            ps.setLong(2, donation.getCardId());
            ps.setDate(3, Dates.toSqlDate(donation.getDonationDate()));
            ps.setString(4, donation.getCentre());
            ps.setInt(5, donation.getVolumeMl());
            ps.setLong(6, donation.getVerifiedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                donation.setDonationId(id);
                return id;
            }
        } catch (SQLException e) {
            throw new DaoException("Could not record the donation", e);
        }
    }

    public List<Donation> findByDonor(Connection conn, long donorId) {
        String sql = SELECT_WITH_JOINS + "WHERE dn.donor_id = ? ORDER BY dn.donation_date ASC, dn.donation_id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, donorId);
            return readAll(ps);
        } catch (SQLException e) {
            throw new DaoException("Could not read history for donor " + donorId, e);
        }
    }

    public List<Donation> findRecent(Connection conn, int limit) {
        String sql = SELECT_WITH_JOINS + "ORDER BY dn.recorded_at DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            return readAll(ps);
        } catch (SQLException e) {
            throw new DaoException("Could not read recent donations", e);
        }
    }

    public int countSince(Connection conn, LocalDate from) {
        String sql = "SELECT COUNT(*) FROM donations WHERE donation_date >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Dates.toSqlDate(from));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Could not count donations", e);
        }
    }

    private List<Donation> readAll(PreparedStatement ps) throws SQLException {
        List<Donation> donations = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                donations.add(map(rs));
            }
        }
        return donations;
    }

    private Donation map(ResultSet rs) throws SQLException {
        Donation donation = new Donation();
        donation.setDonationId(rs.getLong("donation_id"));
        donation.setDonorId(rs.getLong("donor_id"));
        donation.setCardId(rs.getLong("card_id"));
        donation.setDonationDate(Dates.date(rs, "donation_date"));
        donation.setCentre(rs.getString("centre"));
        donation.setVolumeMl(rs.getInt("volume_ml"));
        donation.setVerifiedBy(rs.getLong("verified_by"));
        donation.setRecordedAt(Dates.dateTime(rs, "recorded_at"));
        donation.setDonorName(rs.getString("donor_name"));
        donation.setNationalId(rs.getString("national_id"));
        donation.setBloodGroup(rs.getString("blood_group"));
        donation.setCardNo(rs.getString("card_no"));
        donation.setVerifiedByName(rs.getString("verified_by_name"));
        return donation;
    }
}
