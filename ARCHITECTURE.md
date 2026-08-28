# Architecture — how the system is built and how the parts connect

Blood Donation Card Management System · Section D, Group 3

This document answers two questions: **how the system is put together**, and **which piece talks to which**.

---

## 1. The organising idea: package by feature

Most JEE teaching projects are organised by *layer*:

```
com.example.controller   ← every servlet in the system
com.example.service      ← every service
com.example.dao          ← every DAO
com.example.model        ← every entity
```

This system is organised by **feature** instead. Each feature is a vertical slice that owns its own servlets, service, DAO, model and helpers:

```
com.bdcms.feature.donor
    ├── DonorListServlet.java       controller
    ├── DonorRegisterServlet.java   controller
    ├── DonorViewServlet.java       controller
    ├── DonorPhotoServlet.java      controller
    ├── DonorService.java           business rules
    ├── DonorDao.java               all SQL for the donors table
    ├── Donor.java                  model
    ├── BloodGroup.java  Gender.java
    └── PhotoStore.java             feature-local helper
```

**Why.** "Register a donor" is one change in one directory, not four edits in four unrelated packages. And the rule that keeps the design honest becomes stateable in one line: *a DAO may only touch the tables its own feature owns.* Under a layer layout there is no such rule — every DAO sits next to every other and nothing stops one reaching into another's tables.

The layers still exist. They just run **vertically** (inside a feature) instead of horizontally (across the system).

---

## 2. The map

```
src/main/java/com/bdcms/
│
├── shared/                     ← the only package every feature may depend on
│   ├── db/       Datasource · Tx · DaoException
│   ├── config/   AppConfig · SettingsService · SettingsDao
│   ├── web/      BaseServlet · AuthFilter · CsrfFilter · EncodingFilter
│   │             SessionUser · Csrf · Flash · AppLifecycleListener
│   └── util/     Validate · Dates
│
└── feature/
    ├── auth/          FR-1  login, roles, password hashing
    ├── donor/         FR-2  registration, photo, search, duplicate handling
    ├── card/          FR-3  card numbers, QR codes, issue/reissue, printing
    ├── verification/  FR-4  card lookup, masked ID, PIN, audit log
    ├── eligibility/   FR-5  the waiting-period rule
    ├── donation/      FR-6  transactional recording, history
    ├── report/        FR-7  three reports and PDF export
    ├── dashboard/           read-only summary of everything
    └── admin/               settings and staff accounts
```

**`shared` never imports a feature.** If it ever needed to, the thing it needed would not be shared.

---

## 3. Which feature connects to which

```
                    dashboard  ────────┐  (read-only; nothing depends on it)
                        │              │
                     report            │
                        │              │
                    donation  ─────────┤   FR-6 · the only composer
              ┌─────────┼─────────┐    │
              ▼         ▼         ▼    ▼
       verification  eligibility  card
              │                    │
              └──────►  donor  ◄───┘
                          │
                       shared
```

Read it downwards: an arrow means *imports and calls*. There are no arrows going back up, so there are no cycles.

| Edge | Why it exists |
|---|---|
| `card → donor` | The printed card carries the donor's name, blood group and photo |
| `verification → card` | A card number must resolve to a card before it resolves to a donor |
| `verification → donor` | The screen shows the stored photograph and the masked ID |
| `verification → auth` | Reuses `Passwords.verify` for the donor's PIN — one hashing implementation, not two |
| `donation → verification, eligibility, card` | Composes all three inside one transaction |
| `eligibility → shared only` | Owns no table; it is handed a date and returns a verdict |
| `report → eligibility` | Applies the *configured* rule rather than hard-coding 84 in SQL |

### Two rules that keep it acyclic

**1. Features talk through services, never DAOs.**
`CardServlet` needs donor details, so it calls `donorService.findById(...)`. It never touches `DonorDao`. The donor feature therefore controls what leaves it, and its table stays its own.

**2. Where a code dependency would create a cycle, features are joined by a URL instead.**

Two places do this deliberately:

- **Registration → card issue.** `donor` must not import `card` (`card` already imports `donor`). So after a successful registration, `DonorRegisterServlet` **redirects** to `/app/cards/issue?donorId=N`. Staff get the natural "register, then issue the card" flow; the packages stay independent.
- **Verification → donation.** The confirmation form on `verification/verify.jsp` posts to `/app/donations/record`, owned by the donation feature. Verification does not import donation. This is also what makes the single-transaction requirement achievable — see §5.

---

## 4. A request, end to end

```
Browser
  │  GET /bdcms/app/verify?cardNo=BDC-2026-4KDQ7M2P
  ▼
EncodingFilter          UTF-8 in and out
  ▼
CsrfFilter              GET, so nothing to check
  ▼
AuthFilter              /app/* — is there a session? which role?
  ▼
VerificationServlet     reads the parameter, calls the service
  ▼
VerificationService     .lookup(cardNo)
  ├─► CardService.findByCardNo()   ──► CardDao   ──► donor_cards
  └─► DonorService.findById()      ──► DonorDao  ──► donors
  ▼
VerificationView        card + donor + MASKED id + pinRequired
  ▼
verification/verify.jsp forwarded from /WEB-INF/views (unreachable by URL)
  ▼
Browser                 photograph, masked ID, confirmation box
```

Every screen follows this shape: **Filter → Servlet → Service → DAO → MySQL**, back out as a JSP. Servlets never contain SQL; JSPs never contain logic.

---

## 5. The transaction — the most important 40 lines in the system

The reliability requirement says verification, the eligibility check and the insertion must **all succeed or all roll back**. Here is how that is achieved.

**The enabling design decision:** every DAO method takes a `Connection` as an argument.

```java
public Optional<LocalDate> findLastDonationDate(Connection conn, long donorId)
public long insert(Connection conn, VerificationLog log)
```

Because of that, DAOs belonging to *four different features* can enlist in one transaction — they are all handed the same connection.

```java
// DonationService.record(...)
DonationOutcome outcome = Tx.inTransaction(conn -> {

    // 1. verification: resolves the card, checks status, staff confirmation
    //    and PIN, and writes the audit row — on this connection
    VerificationResult v = verificationService.verify(conn, cardNo, staffId, confirmed, pin);
    if (!v.isSuccess()) return DonationOutcome.verificationFailed(v, ...);

    // 2. the card, for its id
    DonorCard card = cardService.findByCardNo(conn, cardNo).orElseThrow();

    // 3. eligibility, from history read on this same connection
    LocalDate last = donationDao.findLastDonationDate(conn, donorId).orElse(null);
    EligibilityResult e = eligibilityService.evaluate(conn, last);
    if (!e.isEligible()) return DonationOutcome.notEligible(e);

    // 4. append
    donationDao.insert(conn, donation);
    return DonationOutcome.recorded(donation, e);
});
```

`Tx.inTransaction` sets `autoCommit(false)`, commits if the block returns, and rolls back if it throws **anything at all** — checked or unchecked.

### Why a refusal is *returned*, never thrown

A **failed** verification must survive. If it were rolled back with the rest of the unit, the audit trail would contain only the attempts that went well — exactly backwards, since the failures are what supervision is for.

That is achieved by the **return type**, not by a second transaction. Every refusal — unknown card, inactive card, unconfirmed identity, wrong PIN, not eligible — comes back as a `DonationOutcome`. The lambda returns normally, so `Tx` commits: the `verification_log` row is saved, and the donation is simply never inserted. Throwing would roll the audit row back along with everything else.

Only a genuine fault — a dropped connection, a constraint violation — throws. Then the whole unit unwinds, log row included, which is equally correct: in that case nothing happened at all.

> This was originally implemented the other way round: the code assumed a refusal rolled the log row back and re-recorded it in a separate transaction. Running the system proved otherwise — **every failed attempt appeared twice in the audit log**. The re-record path was removed. See [docs/test-evidence.md](docs/test-evidence.md) §5.

---

## 6. Where each requirement lives

| Req | Requirement | Implemented in |
|---|---|---|
| FR-1 | Login, BCrypt, roles | `feature/auth/*`, `shared/web/AuthFilter` |
| FR-2 | Registration, unique national ID, photo | `feature/donor/*`, `UNIQUE(donors.national_id)` |
| FR-3 | Card number, QR, reissue, print | `feature/card/*`, `CardNumberGenerator`, `QrCodeGenerator`, `CardPdfRenderer` |
| FR-4 | Lookup, masked ID, confirmation, PIN, audit | `feature/verification/*`, `IdMasker`, `verification_log` |
| FR-5 | Eligibility vs configured interval | `feature/eligibility/EligibilityService` |
| FR-6 | Transactional recording, history | `feature/donation/DonationService`, `shared/db/Tx` |
| FR-7 | Three reports, PDF | `feature/report/*`, `PdfExporter` |

Two features exist beyond the proposal's seven, both read-only compositions that
nothing depends on — which is why they can import everything without creating a
cycle:

| Feature | What it does |
|---|---|
| `dashboard` | Shift counters, the scan bar, recent activity, today's blood-group mix |
| `admin` | Settings, staff accounts, and the one destructive operation in the system |

| NFR | How |
|---|---|
| Passwords hashed | `Passwords` (BCrypt cost 12). The only class that sees a plaintext secret |
| Prepared statements | Every DAO. No string concatenation of user input anywhere |
| Session timeout 20 min | `web.xml` `<session-timeout>20</session-timeout>` |
| XSS | `<c:out>` / `fn:escapeXml` on every dynamic value |
| CSRF | `CsrfFilter` + per-session token in every POST form |
| Lookup < 2s | `UNIQUE(card_no)`, `UNIQUE(national_id)`, `idx_donations_donor_date` |
| Transactional | §5 above |
| No logic in JSPs | `<scripting-invalid>true</scripting-invalid>` — a scriptlet **fails the build**, verified in §8 |
| Config outside code | `settings` table + `SettingsService`, editable at `/app/admin/settings` |
| Auditability | `verification_log` and `donations.verified_by` both record staff + timestamp |

---

## 7. Decisions worth defending

**Card numbers use Crockford Base32 from `SecureRandom`.** Sequential numbers would let anyone holding one card derive its neighbours, and the card number is the key to a donor's record. The alphabet omits I, L, O and U so a scanner misreading `1` as `I` cannot produce a *different valid* card number. `CardNumberGenerator.normalise` maps those characters back, so a mistyped number still finds the right donor.

**The QR code contains the card number and nothing else.** A card lives in a wallet and can be photographed by a bystander. Encoding a name or national ID would leak it to anyone with a phone; the card number alone is useless without a login.

**The national ID is not on the printed card**, for the same reason.

**Photographs live outside the web root** and are streamed by `DonorPhotoServlet`. They are therefore behind `AuthFilter` like every other donor datum, and an uploaded file can never be executed by the container as a JSP. `PhotoStore` decides the file type by inspecting magic bytes, not by trusting the browser's `Content-Type`.

**Duplicate registration is caught by the database, not by a prior `SELECT`.** Between a check and an insert, another centre could insert the same person. The `UNIQUE` index is the real guard; `DonorService` catches the duplicate-key error and turns it back into the same `DUPLICATE` outcome. That is why "one record per donor" holds under concurrency, which is the whole point of centralising.

**Donations are append-only.** No update, no delete in `DonationDao`. The history *is* the evidence the eligibility rule is computed from; an editable history makes the rule unenforceable.

**Staff accounts are disabled, never deleted** — every donation and verification names the account responsible.

**A donor may be deleted, but only when it costs no history.** `DataAdminService`
refuses if they have any donations, because the eligibility rule is computed
from those rows. When it proceeds, the audit trail is *kept*: verification rows
have their `donor_id` set to null rather than being removed, so every card
number, outcome, staff account and timestamp survives while the link to a person
no longer in the system does not. `verification_log.donor_id` is nullable
precisely so this is possible — it is the only operation in the system that
writes to that table, and it still deletes nothing.

**Webcam scanning is a convenience, never a control.** `bdcms-scanner.js` only
fills in the card-number field; the server then verifies the card, the
photograph, the PIN and eligibility exactly as it does for a typed number. It
prefers the browser's native `BarcodeDetector` and falls back to a bundled jsQR,
so it works offline in any browser.

**`EligibilityService` never queries the donations table.** It is handed a date. That is what lets the rule be tested against a hundred boundary dates in milliseconds with no database at all.

---

## 8. Verification

```bash
mvnw.cmd -P jsp-check clean verify
```

Runs three things:

1. **106 unit tests** — the eligibility rule (boundaries, configurable intervals, future-dated donations), ID masking, card-number format and normalisation, password hashing, input validation, and the transaction contract in `TxTest` (commit on success, rollback on any throw).
2. **JSP translation** — all 18 views are put through Jasper, the same translator Tomcat uses, so a broken taglib URI or a stray scriptlet fails the build rather than surfacing when someone opens the page.
3. **WAR packaging** — `target/bdcms.war`.

The JSP check was confirmed to actually fail: adding `<% int x = 1+1; %>` to a view produced

```
Scripting elements ( <%!, <jsp:declaration, <%=, ... ) are disallowed here.
[INFO] BUILD FAILURE
```

which is the `<scripting-invalid>` rule being enforced rather than merely claimed.

See [SETUP.md](SETUP.md) to run it, and [docs/er-diagram.md](docs/er-diagram.md) for the schema.
