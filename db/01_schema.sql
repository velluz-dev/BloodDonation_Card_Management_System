-- ---------------------------------------------------------------------------
-- Blood Donation Card Management System - schema
-- MySQL 8.0 / InnoDB / utf8mb4
--
-- Run first:   mysql -u root -p < db/01_schema.sql
-- Then seed:   mysql -u root -p < db/02_seed.sql
-- ---------------------------------------------------------------------------

DROP DATABASE IF EXISTS bdcms;
CREATE DATABASE bdcms
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE bdcms;

-- ---------------------------------------------------------------------------
-- users - administrator and staff accounts (FR-1)
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    user_id       BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(40)  NOT NULL,
    password_hash CHAR(60)     NOT NULL COMMENT 'BCrypt hash - never plaintext',
    full_name     VARCHAR(120) NOT NULL,
    role          ENUM('ADMIN','STAFF') NOT NULL DEFAULT 'STAFF',
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_username (username)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- donors - one row per person (FR-2)
--
-- uq_donors_national_id is the primary defence against the duplicate
-- registration problem described in Section 2 of the proposal. It is enforced
-- by the database, so two centres registering the same person concurrently
-- cannot both succeed.
-- ---------------------------------------------------------------------------
CREATE TABLE donors (
    donor_id      BIGINT       NOT NULL AUTO_INCREMENT,
    national_id   VARCHAR(20)  NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    date_of_birth DATE         NOT NULL,
    gender        ENUM('MALE','FEMALE','OTHER') NOT NULL,
    blood_group   ENUM('A_POS','A_NEG','B_POS','B_NEG','AB_POS','AB_NEG','O_POS','O_NEG') NOT NULL,
    contact_no    VARCHAR(20)  NOT NULL,
    address       VARCHAR(255)     NULL,
    photo_path    VARCHAR(255)     NULL COMMENT 'Relative path under the configured upload dir',
    pin_hash      CHAR(60)         NULL COMMENT 'Optional 4-digit PIN, BCrypt hashed (proposal S10)',
    registered_on DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (donor_id),
    UNIQUE KEY uq_donors_national_id (national_id),
    KEY idx_donors_full_name (full_name),
    KEY idx_donors_blood_group (blood_group)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- donor_cards - issued cards (FR-3)
--
-- A donor accumulates card rows over time (lost, reissued), but the
-- application invariant is that at most one is ACTIVE. Donation history hangs
-- off the donor, not the card, so reissuing a card never orphans history.
-- ---------------------------------------------------------------------------
CREATE TABLE donor_cards (
    card_id   BIGINT      NOT NULL AUTO_INCREMENT,
    card_no   VARCHAR(32) NOT NULL,
    donor_id  BIGINT      NOT NULL,
    issued_on DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status    ENUM('ACTIVE','INACTIVE','LOST') NOT NULL DEFAULT 'ACTIVE',
    issued_by BIGINT          NULL,
    PRIMARY KEY (card_id),
    UNIQUE KEY uq_cards_card_no (card_no),
    KEY idx_cards_donor_status (donor_id, status),
    CONSTRAINT fk_cards_donor  FOREIGN KEY (donor_id)  REFERENCES donors (donor_id),
    CONSTRAINT fk_cards_issuer FOREIGN KEY (issued_by) REFERENCES users (user_id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- donations - append-only donation history (FR-6)
--
-- idx_donations_donor_date makes "the donor's most recent donation" - the query
-- behind every eligibility check - a single index seek, which is what keeps
-- lookup inside the 2 second performance requirement.
-- ---------------------------------------------------------------------------
CREATE TABLE donations (
    donation_id   BIGINT      NOT NULL AUTO_INCREMENT,
    donor_id      BIGINT      NOT NULL,
    card_id       BIGINT      NOT NULL,
    donation_date DATE        NOT NULL,
    centre        VARCHAR(120) NOT NULL,
    volume_ml     INT         NOT NULL DEFAULT 450,
    verified_by   BIGINT      NOT NULL COMMENT 'Staff account accountable for this donation',
    recorded_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (donation_id),
    KEY idx_donations_donor_date (donor_id, donation_date DESC),
    KEY idx_donations_date (donation_date),
    CONSTRAINT fk_donations_donor    FOREIGN KEY (donor_id)    REFERENCES donors (donor_id),
    CONSTRAINT fk_donations_card     FOREIGN KEY (card_id)     REFERENCES donor_cards (card_id),
    CONSTRAINT fk_donations_verifier FOREIGN KEY (verified_by) REFERENCES users (user_id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- verification_log - audit trail (FR-4, NFR Auditability)
--
-- Every attempt is recorded, successful or not. card_no is stored as text
-- rather than a foreign key so that an attempt with a card number that does not
-- exist at all is still auditable; donor_id is nullable for the same reason.
-- ---------------------------------------------------------------------------
CREATE TABLE verification_log (
    log_id       BIGINT      NOT NULL AUTO_INCREMENT,
    card_no      VARCHAR(32) NOT NULL,
    donor_id     BIGINT          NULL,
    staff_id     BIGINT      NOT NULL,
    result       ENUM('SUCCESS','CARD_NOT_FOUND','CARD_INACTIVE','PIN_MISMATCH','NOT_CONFIRMED')
                 NOT NULL,
    detail       VARCHAR(255)    NULL,
    attempted_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_vlog_attempted (attempted_at),
    KEY idx_vlog_donor (donor_id),
    KEY idx_vlog_card_no (card_no),
    CONSTRAINT fk_vlog_donor FOREIGN KEY (donor_id) REFERENCES donors (donor_id),
    CONSTRAINT fk_vlog_staff FOREIGN KEY (staff_id) REFERENCES users (user_id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- settings - runtime-configurable business rules (NFR Maintainability)
--
-- The minimum donation interval lives here rather than in Java, so a centre can
-- change the rule without a redeployment.
-- ---------------------------------------------------------------------------
CREATE TABLE settings (
    setting_key   VARCHAR(64)  NOT NULL,
    setting_value VARCHAR(255) NOT NULL,
    description   VARCHAR(255)     NULL,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key)
) ENGINE = InnoDB;
