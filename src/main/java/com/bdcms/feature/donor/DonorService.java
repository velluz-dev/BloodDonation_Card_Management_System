package com.bdcms.feature.donor;

import com.bdcms.feature.auth.Passwords;
import com.bdcms.shared.db.Tx;
import com.bdcms.shared.util.Validate;
import jakarta.servlet.http.Part;

import java.sql.Connection;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Optional;

/**
 * Donor registration and lookup rules (FR-2).
 *
 * <p>The interesting requirement here is duplicate handling. The proposal asks
 * that a repeat national ID be <em>rejected and the existing record shown</em>,
 * which is a normal outcome of the workflow rather than an error, so
 * {@link #register} returns a {@link Result} carrying the existing donor
 * instead of throwing.
 *
 * <p>The check is deliberately not "select then insert": between those two
 * statements another centre could insert the same person. The unique index on
 * {@code donors.national_id} is the real guard, and the duplicate-key error it
 * raises is translated back into the same {@code DUPLICATE} outcome. That is
 * why the proposal's promise of one record per donor holds under concurrency.
 */
public class DonorService {

    /** Why a registration attempt ended the way it did. */
    public enum Outcome {
        REGISTERED,
        DUPLICATE,
        INVALID
    }

    /** Result of {@link #register}: the outcome plus whichever detail applies. */
    public record Result(Outcome outcome, Donor donor, Donor existing, Validate.Errors errors) {

        public boolean isRegistered() {
            return outcome == Outcome.REGISTERED;
        }

        public boolean isDuplicate() {
            return outcome == Outcome.DUPLICATE;
        }

        static Result registered(Donor donor) {
            return new Result(Outcome.REGISTERED, donor, null, new Validate.Errors());
        }

        static Result duplicate(Donor existing) {
            return new Result(Outcome.DUPLICATE, null, existing, new Validate.Errors());
        }

        static Result invalid(Validate.Errors errors) {
            return new Result(Outcome.INVALID, null, null, errors);
        }
    }

    private final DonorDao donorDao;

    public DonorService() {
        this(new DonorDao());
    }

    public DonorService(DonorDao donorDao) {
        this.donorDao = donorDao;
    }

    /**
     * Registers a donor, optionally with a photograph and a four-digit PIN.
     *
     * <p>The photo is written to disk only after the row is inserted, so a
     * rejected registration never leaves an orphaned file behind.
     */
    public Result register(Donor donor, String plainPin, Part photo) {
        Validate.Errors errors = validate(donor, plainPin);
        if (errors.hasErrors()) {
            return Result.invalid(errors);
        }

        donor.setNationalId(Validate.normaliseNationalId(donor.getNationalId()));
        if (plainPin != null && !plainPin.isBlank()) {
            donor.setPinHash(Passwords.hash(plainPin));
        }

        Optional<Donor> existing = findByNationalId(donor.getNationalId());
        if (existing.isPresent()) {
            return Result.duplicate(existing.get());
        }

        try {
            Tx.inTransaction(conn -> {
                donorDao.insert(conn, donor);
                if (PhotoStore.isPresent(photo)) {
                    donor.setPhotoPath(PhotoStore.save(photo, donor.getDonorId()));
                    donorDao.updatePhotoPath(conn, donor.getDonorId(), donor.getPhotoPath());
                }
                return null;
            });
        } catch (RuntimeException e) {
            // The unique index fired: someone else inserted this national ID
            // between our check and our insert. Report it as the duplicate it is.
            if (isDuplicateKey(e)) {
                return findByNationalId(donor.getNationalId())
                        .map(Result::duplicate)
                        .orElseGet(() -> Result.duplicate(null));
            }
            throw e;
        }
        return Result.registered(donor);
    }

    public Optional<Donor> findById(long donorId) {
        return Tx.read(conn -> donorDao.findById(conn, donorId));
    }

    public Optional<Donor> findById(Connection conn, long donorId) {
        return donorDao.findById(conn, donorId);
    }

    public Optional<Donor> findByNationalId(String nationalId) {
        String normalised = Validate.normaliseNationalId(nationalId);
        return Tx.read(conn -> donorDao.findByNationalId(conn, normalised));
    }

    public List<Donor> search(String query, BloodGroup bloodGroup, int limit, int offset) {
        return Tx.read(conn -> donorDao.search(conn, query, bloodGroup, limit, offset));
    }

    public int countSearch(String query, BloodGroup bloodGroup) {
        return Tx.read(conn -> donorDao.countSearch(conn, query, bloodGroup));
    }

    public int countAll() {
        return Tx.read(donorDao::countAll);
    }

    /** Updates the editable demographic fields. The national ID is never changed. */
    public Validate.Errors update(Donor donor, Part photo) {
        Validate.Errors errors = validateEditable(donor);
        if (errors.hasErrors()) {
            return errors;
        }
        Tx.inTransaction(conn -> {
            donorDao.update(conn, donor);
            if (PhotoStore.isPresent(photo)) {
                String path = PhotoStore.save(photo, donor.getDonorId());
                donor.setPhotoPath(path);
                donorDao.updatePhotoPath(conn, donor.getDonorId(), path);
            }
            return null;
        });
        return errors;
    }

    /** Sets or clears the optional verification PIN. Passing null clears it. */
    public void setPin(long donorId, String plainPin) {
        String hash = (plainPin == null || plainPin.isBlank()) ? null : Passwords.hash(plainPin);
        Tx.inTransaction(conn -> {
            donorDao.updatePinHash(conn, donorId, hash);
            return null;
        });
    }

    private Validate.Errors validate(Donor donor, String plainPin) {
        Validate.Errors errors = validateEditable(donor);
        errors.check(Validate.isNationalId(donor.getNationalId()), "nationalId",
                "National ID is required (5-20 letters, digits or hyphens).");
        errors.check(plainPin == null || plainPin.isBlank() || Validate.isPin(plainPin), "pin",
                "PIN must be exactly four digits.");
        return errors;
    }

    private Validate.Errors validateEditable(Donor donor) {
        Validate.Errors errors = new Validate.Errors();
        errors.check(Validate.isPresent(donor.getFullName()), "fullName", "Full name is required.");
        errors.check(Validate.maxLength(donor.getFullName(), 120), "fullName",
                "Full name must be 120 characters or fewer.");
        errors.check(Validate.isDonorAge(donor.getDateOfBirth(), java.time.LocalDate.now()),
                "dateOfBirth",
                "Donor must be between " + Validate.MIN_DONOR_AGE + " and "
                        + Validate.MAX_DONOR_AGE + " years old.");
        errors.check(donor.getGender() != null, "gender", "Gender is required.");
        errors.check(donor.getBloodGroup() != null, "bloodGroup", "Blood group is required.");
        errors.check(Validate.isPhone(donor.getContactNo()), "contactNo",
                "Contact number is not a valid phone number.");
        errors.check(Validate.maxLength(donor.getAddress(), 255), "address",
                "Address must be 255 characters or fewer.");
        return errors;
    }

    /** Walks the cause chain looking for MySQL's duplicate-key condition. */
    private boolean isDuplicateKey(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (cause instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }
}
