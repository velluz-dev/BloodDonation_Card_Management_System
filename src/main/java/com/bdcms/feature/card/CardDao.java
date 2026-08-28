package com.bdcms.feature.card;

import com.bdcms.shared.db.DaoException;
import com.bdcms.shared.util.Dates;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** All SQL against the {@code donor_cards} table (FR-3). */
public class CardDao {

    private static final String COLUMNS =
            "card_id, card_no, donor_id, issued_on, status, issued_by";

    public Optional<DonorCard> findByCardNo(Connection conn, String cardNo) {
        String sql = "SELECT " + COLUMNS + " FROM donor_cards WHERE card_no = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cardNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not look up card " + cardNo, e);
        }
    }

    public Optional<DonorCard> findById(Connection conn, long cardId) {
        String sql = "SELECT " + COLUMNS + " FROM donor_cards WHERE card_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load card " + cardId, e);
        }
    }

    /** The donor's current card, if they have one. */
    public Optional<DonorCard> findActiveByDonor(Connection conn, long donorId) {
        String sql = "SELECT " + COLUMNS + " FROM donor_cards "
                + "WHERE donor_id = ? AND status = 'ACTIVE' ORDER BY issued_on DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load active card for donor " + donorId, e);
        }
    }

    /** Every card ever issued to a donor, newest first - the reissue trail. */
    public List<DonorCard> findAllByDonor(Connection conn, long donorId) {
        String sql = "SELECT " + COLUMNS + " FROM donor_cards "
                + "WHERE donor_id = ? ORDER BY issued_on DESC, card_id DESC";
        List<DonorCard> cards = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cards.add(map(rs));
                }
            }
            return cards;
        } catch (SQLException e) {
            throw new DaoException("Could not list cards for donor " + donorId, e);
        }
    }

    public long insert(Connection conn, DonorCard card) throws SQLException {
        String sql = "INSERT INTO donor_cards (card_no, donor_id, status, issued_by) VALUES (?, ?, ?, ?)";
        // Deliberately lets SQLException escape: CardService needs to see the
        // duplicate-key error so it can retry with a fresh card number.
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, card.getCardNo());
            ps.setLong(2, card.getDonorId());
            ps.setString(3, card.getStatus().name());
            if (card.getIssuedBy() == null) {
                ps.setNull(4, java.sql.Types.BIGINT);
            } else {
                ps.setLong(4, card.getIssuedBy());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                card.setCardId(id);
                return id;
            }
        }
    }

    public void updateStatus(Connection conn, long cardId, CardStatus status) {
        String sql = "UPDATE donor_cards SET status = ? WHERE card_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, cardId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Could not change status of card " + cardId, e);
        }
    }

    /**
     * Marks every currently active card of a donor with {@code status}.
     * Called immediately before issuing a replacement, which is what keeps the
     * "at most one ACTIVE card per donor" invariant true.
     */
    public int deactivateAllActive(Connection conn, long donorId, CardStatus status) {
        String sql = "UPDATE donor_cards SET status = ? WHERE donor_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, donorId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Could not deactivate cards for donor " + donorId, e);
        }
    }

    /**
     * Removes every card belonging to a donor.
     *
     * <p>Part of deleting a donor. A card is meaningless without the person it
     * identifies, and the audit trail records card numbers as text rather than
     * as a foreign key, so the log stays readable after the rows are gone.
     */
    public int deleteAllByDonor(Connection conn, long donorId) {
        String sql = "DELETE FROM donor_cards WHERE donor_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, donorId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Could not delete cards for donor " + donorId, e);
        }
    }

    /** How many cards currently sit in a given state. Feeds the dashboard rail. */
    public int countByStatus(Connection conn, CardStatus status) {
        String sql = "SELECT COUNT(*) FROM donor_cards WHERE status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Could not count " + status + " cards", e);
        }
    }

    public int countActive(Connection conn) {
        String sql = "SELECT COUNT(*) FROM donor_cards WHERE status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DaoException("Could not count active cards", e);
        }
    }

    private DonorCard map(ResultSet rs) throws SQLException {
        DonorCard card = new DonorCard();
        card.setCardId(rs.getLong("card_id"));
        card.setCardNo(rs.getString("card_no"));
        card.setDonorId(rs.getLong("donor_id"));
        card.setIssuedOn(Dates.dateTime(rs, "issued_on"));
        card.setStatus(CardStatus.parse(rs.getString("status")).orElse(CardStatus.INACTIVE));
        long issuedBy = rs.getLong("issued_by");
        card.setIssuedBy(rs.wasNull() ? null : issuedBy);
        return card;
    }
}
