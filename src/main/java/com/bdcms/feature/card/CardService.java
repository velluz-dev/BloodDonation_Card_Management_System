package com.bdcms.feature.card;

import com.bdcms.shared.db.DaoException;
import com.bdcms.shared.db.Tx;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Card issue, reissue and withdrawal (FR-3).
 *
 * <p>The public entry points come in two shapes on purpose:
 * <ul>
 *   <li>The no-{@code Connection} methods open their own transaction and are
 *       what the card screens call.</li>
 *   <li>The {@code Connection}-taking methods join a transaction someone else
 *       started. That is how the donation feature can read a card inside the
 *       same unit of work that writes the donation - without ever touching
 *       {@link CardDao} itself.</li>
 * </ul>
 */
public class CardService {

    /** Card numbers are random; on the rare collision, try again rather than fail the issue. */
    private static final int MAX_ATTEMPTS = 5;

    private final CardDao cardDao;
    private final CardNumberGenerator numberGenerator;

    public CardService() {
        this(new CardDao(), new CardNumberGenerator());
    }

    public CardService(CardDao cardDao, CardNumberGenerator numberGenerator) {
        this.cardDao = cardDao;
        this.numberGenerator = numberGenerator;
    }

    // ---- queries -----------------------------------------------------------

    /** Looks a card up by number, tolerating operator typing and scanner quirks. */
    public Optional<DonorCard> findByCardNo(String cardNo) {
        return Tx.read(conn -> findByCardNo(conn, cardNo));
    }

    /** As above, joining an existing transaction. */
    public Optional<DonorCard> findByCardNo(Connection conn, String cardNo) {
        String normalised = CardNumberGenerator.normalise(cardNo);
        if (normalised == null || normalised.isBlank()) {
            return Optional.empty();
        }
        return cardDao.findByCardNo(conn, normalised);
    }

    public Optional<DonorCard> findActiveByDonor(long donorId) {
        return Tx.read(conn -> cardDao.findActiveByDonor(conn, donorId));
    }

    public Optional<DonorCard> findActiveByDonor(Connection conn, long donorId) {
        return cardDao.findActiveByDonor(conn, donorId);
    }

    public List<DonorCard> historyForDonor(long donorId) {
        return Tx.read(conn -> cardDao.findAllByDonor(conn, donorId));
    }

    public Optional<DonorCard> findById(long cardId) {
        return Tx.read(conn -> cardDao.findById(conn, cardId));
    }

    public int countActive() {
        return Tx.read(cardDao::countActive);
    }

    /** How many cards are currently in a given state. */
    public int countByStatus(CardStatus status) {
        return Tx.read(conn -> cardDao.countByStatus(conn, status));
    }

    // ---- commands ----------------------------------------------------------

    /**
     * Issues a card to a donor, replacing any card they currently hold.
     *
     * <p>Deactivation and insertion happen in one transaction, so there is no
     * instant at which a donor has two active cards or none at all.
     *
     * @param supersededStatus what to mark the previous card as -
     *                         {@link CardStatus#LOST} for a lost-card reissue,
     *                         {@link CardStatus#INACTIVE} for a routine replacement
     */
    public DonorCard issue(long donorId, long issuedByUserId, CardStatus supersededStatus) {
        return Tx.inTransaction(conn -> issue(conn, donorId, issuedByUserId, supersededStatus));
    }

    /** As above, joining an existing transaction. */
    public DonorCard issue(Connection conn, long donorId, long issuedByUserId,
                           CardStatus supersededStatus) throws SQLException {

        cardDao.deactivateAllActive(conn, donorId,
                supersededStatus == null ? CardStatus.INACTIVE : supersededStatus);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            DonorCard card = new DonorCard();
            card.setCardNo(numberGenerator.next());
            card.setDonorId(donorId);
            card.setStatus(CardStatus.ACTIVE);
            card.setIssuedBy(issuedByUserId);

            try {
                cardDao.insert(conn, card);
                return card;
            } catch (SQLException e) {
                // InnoDB rolls back only the failing statement on a duplicate
                // key, not the whole transaction, so retrying on the same
                // connection is safe.
                if (isDuplicateCardNo(e) && attempt < MAX_ATTEMPTS) {
                    continue;
                }
                throw e;
            }
        }
        throw new DaoException("Could not allocate a unique card number after "
                + MAX_ATTEMPTS + " attempts");
    }

    /** Marks a card lost. It stops working immediately; the donor's history is untouched. */
    public void reportLost(long cardId) {
        setStatus(cardId, CardStatus.LOST);
    }

    /** Withdraws a card without declaring it lost. */
    public void deactivate(long cardId) {
        setStatus(cardId, CardStatus.INACTIVE);
    }

    private void setStatus(long cardId, CardStatus status) {
        Tx.inTransaction(conn -> {
            cardDao.updateStatus(conn, cardId, status);
            return null;
        });
    }

    private boolean isDuplicateCardNo(SQLException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getErrorCode() == 1062) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }
}
