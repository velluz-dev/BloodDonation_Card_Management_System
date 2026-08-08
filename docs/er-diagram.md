# Entity-relationship diagram

Blood Donation Card Management System · Phase 1 deliverable

```mermaid
erDiagram
    USERS ||--o{ DONOR_CARDS : "issues"
    USERS ||--o{ DONATIONS : "verifies"
    USERS ||--o{ VERIFICATION_LOG : "attempts"

    DONORS ||--o{ DONOR_CARDS : "holds over time"
    DONORS ||--o{ DONATIONS : "gives"
    DONORS ||--o{ VERIFICATION_LOG : "is subject of"

    DONOR_CARDS ||--o{ DONATIONS : "presented for"

    USERS {
        bigint  user_id        PK
        varchar username       UK "unique login"
        char    password_hash     "BCrypt, 60 chars"
        varchar full_name
        enum    role              "ADMIN | STAFF"
        bool    is_active         "disabled, never deleted"
        datetime created_at
    }

    DONORS {
        bigint   donor_id      PK
        varchar  national_id   UK "THE duplicate-registration guard"
        varchar  full_name
        date     date_of_birth
        enum     gender           "MALE | FEMALE | OTHER"
        enum     blood_group      "A_POS .. O_NEG"
        varchar  contact_no
        varchar  address
        varchar  photo_path       "relative path, file lives outside web root"
        char     pin_hash         "optional 4-digit PIN, BCrypt hashed"
        datetime registered_on
    }

    DONOR_CARDS {
        bigint   card_id       PK
        varchar  card_no       UK "BDC-yyyy-XXXXXXXX, in the QR code"
        bigint   donor_id      FK
        datetime issued_on
        enum     status           "ACTIVE | INACTIVE | LOST"
        bigint   issued_by     FK "users.user_id"
    }

    DONATIONS {
        bigint   donation_id   PK
        bigint   donor_id      FK
        bigint   card_id       FK
        date     donation_date    "drives the eligibility rule"
        varchar  centre
        int      volume_ml
        bigint   verified_by   FK "accountable staff account"
        datetime recorded_at
    }

    VERIFICATION_LOG {
        bigint   log_id        PK
        varchar  card_no          "text, not FK - unknown cards are auditable too"
        bigint   donor_id      FK "nullable, for the same reason"
        bigint   staff_id      FK
        enum     result           "SUCCESS | CARD_NOT_FOUND | CARD_INACTIVE | PIN_MISMATCH | NOT_CONFIRMED"
        varchar  detail
        datetime attempted_at
    }

    SETTINGS {
        varchar  setting_key   PK "min_donation_interval_days = 84"
        varchar  setting_value
        varchar  description
        datetime updated_at
    }
```

`SETTINGS` stands alone by design — it holds the business rules, not data about any entity.

---

## Constraints that do real work

| Constraint | What it prevents |
|---|---|
| `UNIQUE (donors.national_id)` | Duplicate registration — the core problem in Section 2 of the proposal. Enforced by the database, so two centres registering the same person concurrently cannot both succeed |
| `UNIQUE (donor_cards.card_no)` | Two donors sharing a card number. Also the backstop the card generator retries against |
| `FK donations.verified_by → users` | A donation without an accountable staff account |
| `FK verification_log.staff_id → users` | An unattributable audit entry |
| `verification_log.donor_id` **nullable** | Deliberately permits logging an attempt with a card number that matches no donor — otherwise the most suspicious attempts would be the ones that could not be recorded |

## Indexes and why

| Index | Query it serves |
|---|---|
| `uq_donors_national_id` | Duplicate check at registration |
| `uq_cards_card_no` | **Card lookup at the counter** — the 2-second performance requirement |
| `idx_donations_donor_date (donor_id, donation_date DESC)` | "The donor's most recent donation" — a single index seek, the query the entire eligibility rule rests on |
| `idx_cards_donor_status (donor_id, status)` | Finding a donor's one ACTIVE card |
| `idx_vlog_attempted (attempted_at)` | Audit log paging by date |
| `idx_donors_full_name`, `idx_donors_blood_group` | Donor search and the blood-group report |

## Application invariants

Some rules cannot be expressed as constraints and are enforced in code instead:

- **At most one ACTIVE card per donor.** `CardService.issue` deactivates existing cards and inserts the replacement in one transaction, so there is no moment with two or with none. (A partial unique index would express this in PostgreSQL; MySQL has no equivalent.)
- **Donations are append-only.** `DonationDao` has no update or delete method. The history is the evidence the eligibility rule is computed from.
- **History belongs to the donor, not the card.** `donations.donor_id` is what survives a reissue; `card_id` records only which card was presented on the day.
