# Test evidence

Blood Donation Card Management System · Phase 6 deliverable

Captured from an actual run on Windows 11, JDK 25 (Temurin), Maven 3.9.9.

```
mvnw.cmd -P jsp-check clean verify
```

---

## 1. Unit tests — 106 passed, 0 failed

```
[INFO] Running com.bdcms.feature.auth.PasswordsTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.354 s
[INFO] Running com.bdcms.feature.card.CardNumberGeneratorTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.147 s
[INFO] Running com.bdcms.feature.eligibility.EligibilityServiceTest$BadInput
[INFO] Tests run:  4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s
[INFO] Running com.bdcms.feature.eligibility.EligibilityServiceTest$ConfigurableInterval
[INFO] Tests run:  8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.033 s
[INFO] Running com.bdcms.feature.eligibility.EligibilityServiceTest$NextEligibleDate
[INFO] Tests run:  3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s
[INFO] Running com.bdcms.feature.eligibility.EligibilityServiceTest$Boundary
[INFO] Tests run:  4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s
[INFO] Running com.bdcms.feature.eligibility.EligibilityServiceTest$FirstDonation
[INFO] Tests run:  2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s
[INFO] Running com.bdcms.feature.verification.IdMaskerTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.027 s
[INFO] Running com.bdcms.shared.config.AppConfigTest
[INFO] Tests run:  5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s
[INFO] Running com.bdcms.shared.config.SettingsServiceTest
[INFO] Tests run:  5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s
[INFO] Running com.bdcms.shared.db.TxTest
[INFO] Tests run:  7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.031 s
[INFO] Running com.bdcms.shared.util.ValidateTest
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.036 s

[INFO] Results:
[INFO] Tests run: 106, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Every test runs without a database, a container or a real clock.** That is a consequence of the architecture, not an accident: `EligibilityService` is handed a date instead of querying for one, so `today` can be passed in and every boundary stated directly.

---

## 2. What each suite covers

### `EligibilityServiceTest` — 21 tests (FR-5)

The rule that protects donor health, so the boundary is tested from both sides.

| Case | Expected |
|---|---|
| No previous donation | ELIGIBLE (first-time donor) |
| **Exactly 84 days** since last | **ELIGIBLE** — the waiting period is complete |
| **83 days** — one day short | **NOT ELIGIBLE** |
| 85 days | ELIGIBLE |
| Donated today | NOT ELIGIBLE |
| Next permitted date | last donation + interval |
| Days remaining | counted from today, not from the last donation |
| Configurable interval | 7 parameterised cases across 1, 56, 84, 120 day intervals |
| Donation dated in the future | NOT ELIGIBLE — not treated as an enormous gap |
| Negative interval / null date | `IllegalArgumentException` |

### `IdMaskerTest` — 14 tests (FR-4)

| Input | Output |
|---|---|
| `S1234567A` | `S••••567A` — the proposal's own example |
| `AB1234567` | `A••••4567` |
| `A` … `ABCDE` | fully masked — too short to reveal a tail safely |
| `null`, `""`, `"   "` | `""` — never the string "null" |

Also asserted: the middle never leaks, and the masked form is the same length as the original so nothing can be inferred from it.

### `CardNumberGeneratorTest` — 15 tests (FR-3)

- Format `BDC-2026-XXXXXXXX`.
- **10,000 generated numbers, all distinct.**
- Never emits **I, L, O or U** — the characters a scanner or a person confuses with 1 and 0.
- Malformed input rejected: wrong prefix, wrong length, excluded characters, missing separators, `null`.
- Normalisation upper-cases, trims, and maps `I`/`L` → `1`, `O` → `0`, so a mistyped number still finds the right donor.

### `PasswordsTest` — 10 tests (FR-1, NFR Security)

- Hash is never the plaintext and does not contain it.
- Correct password verifies; wrong password, wrong case and empty string do not.
- **The same password hashed twice gives different hashes** — the salt is random, so a stolen database cannot be attacked with one rainbow table.
- Hash is exactly 60 characters, matching the `CHAR(60)` column.
- A null or malformed hash fails closed rather than throwing — a corrupt row looks exactly like a wrong password.
- The same mechanism hashes the 4-digit donor PIN.
- **The seeded `admin` hash in `db/02_seed.sql` really does verify against `Admin@123`**, so the seed file and the login page cannot drift apart.

### `TxTest` — 7 tests (NFR Reliability)

The transaction contract the donation flow depends on, using a proxy `Connection` that records which methods were called.

| Case | Asserted |
|---|---|
| Work returns normally | `commit` called, `rollback` not, connection closed |
| Transaction opened | `setAutoCommit(false)` called |
| Work throws `SQLException` | `rollback` called, `commit` **not**, surfaces as `DaoException` |
| Work throws `RuntimeException` | rolled back, and **rethrown unchanged** so the caller sees the real failure |
| `read()` | no commit — a query needs no transaction |
| Used before the pool exists | `IllegalStateException`, not a silent failure |

### `ValidateTest` — 29 tests (FR-2)

National ID formats, phone formats, 4-digit PINs, donor age bounds (16–70, plus unborn and born-today), error accumulation so a form shows every problem at once, and **national ID normalisation** — without which `s1234567a` and `S1234567A` would be two donors and the unique index would not catch the duplicate.

---

## 3. JSP translation — 18 views, 0 errors

Every JSP is put through Jasper, the same translator Tomcat uses, so a broken taglib URI or a stray scriptlet fails the build rather than surfacing when a user opens the page.

```
INFO: Generation completed with [0] errors in [1173] milliseconds
[INFO] BUILD SUCCESS
```

### Two real defects this caught

**1. Taglib prefix used before it was declared.**

```
verification/audit.jsp (line: [7], column: [0])
layout/header.jspf (line: [9], column: [46])
The prefix [c] specified in this tag directive has been previously used by an
action in file [/WEB-INF/views/verification/audit.jsp] line [6].
```

Every page set its own title with `<c:set>` *before* including the header that declared the `c` prefix. Fixed by hoisting the directives into `layout/taglibs.jspf`, included first by every view. This would have produced a 500 on every page at runtime.

**2. `empty` is an EL reserved word.**

```
report/reports.jsp (line: [75], column: [8])
[${report.empty}] contains invalid expression(s):
[jakarta.el.ELException: Failed to parse the expression [${report.empty}]]
```

`Report.isEmpty()` cannot be read as `${report.empty}`. Changed to `${empty report.rows}`.

---

## 4. Proof that the "no logic in JSPs" rule is enforced

The maintainability requirement says JSP pages contain no business logic. That is enforced by `<scripting-invalid>true</scripting-invalid>` in `web.xml` — but a control nobody has tested is only a claim. So it was tested by deliberately adding a scriptlet to `dashboard.jsp`:

```jsp
<% int businessLogic = 1 + 1; out.print(businessLogic); %>
```

Result:

```
dashboard.jsp (line: [123], column: [2])
Scripting elements ( <%!, <jsp:declaration, <%=, <jsp:expression, <%,
<jsp:scriptlet ) are disallowed here.
[INFO] BUILD FAILURE
```

The scriptlet was removed and the build returned to green. Java code cannot be committed into a view in this project — it fails to build.

---

## 5. Live end-to-end run — 56 checks, all passing

The application was deployed to embedded Tomcat 10.1.30 against a real MySQL
8.0.37 instance and driven over HTTP: log in, look up cards, confirm identity,
record donations, export PDFs, read the audit log, and exercise the guards on
administrator deletion.

The suite lives in the repository at [`scripts/smoke-test.ps1`](../scripts/smoke-test.ps1)
and is re-runnable:

```powershell
mysql ... < db/01_schema.sql
mysql ... < db/02_seed.sql
.\scripts\smoke-test.ps1
```

```
=== 1.  Access control ===          anonymous redirect, destination preserved
=== 2.  Login page and CSRF ===     token issued, wrong password 401, no-token POST 403
=== 3.  Sign in as admin ===        dashboard renders, Administration menu present
=== 4.  Donor search (FR-2) ===     list renders, search filters
=== 5.  Verification (FR-4) ===     card resolves, ID masked, FULL ID never sent,
                                    PIN demanded, confirmation required,
                                    unknown card handled, LOST card refused
=== 6.  Donation (FR-6) ===         recorded; wrong PIN 400; unconfirmed refused
=== 7.  Eligibility (FR-5) ===      30/84 days refused 409 with next date;
                                    84 days ELIGIBLE; 83 days NOT ELIGIBLE
=== 8.  Cards (FR-3) ===            reissue trail, QR PNG, card PDF, photo servlet
=== 9.  Reports (FR-7) ===          all three render and export to PDF
=== 10. Audit and settings ===      success and failure rows present
=== 11. Admin data management ===   delete control admin-only; donor with
                                    donations refused; CSRF enforced;
                                    a GET never deletes
=== 12. Roles (FR-1) ===            STAFF gets 403 on /app/admin, no admin menu,
                                    no delete control, delete endpoint refused

================  56 passed, 0 failed  ================
```

### Two defects only a live run could catch

**1. Records are not JavaBeans to Jakarta EL 5.0.** Every HTML page returned 500, while the PDF and image endpoints worked:

```
SEVERE: Servlet.service() for servlet [dashboardServlet] threw exception
[An exception occurred processing [/WEB-INF/views/layout/header.jspf] at line [69]
69:  <c:out value="${currentUser.fullName}" />
```

`SessionUser` is a record, so its accessor is `fullName()`. Tomcat 10.1 implements **Jakarta EL 5.0, which cannot read record components as bean properties** — that arrived in EL 6.0. Jasper precompilation passes because translation is fine; the failure is at EL resolution time. JavaBean getters were added to the five records used in views (`SessionUser`, `VerificationView`, `EligibilityResult`, `Report`, `ReportRow`).

**2. Every failed verification was logged twice.** The database showed:

```
BDC-2026-4KDQ7M2P | PIN_MISMATCH  | admin
BDC-2026-4KDQ7M2P | PIN_MISMATCH  | admin      <-- duplicate
BDC-2026-4KDQ7M2P | NOT_CONFIRMED | admin
BDC-2026-4KDQ7M2P | NOT_CONFIRMED | admin      <-- duplicate
```

`DonationService` assumed a refusal rolled the `verification_log` row back and re-recorded it in a second transaction. But a refusal is *returned*, not thrown — so the block completes normally and `Tx` **commits** the row. The re-record was pure duplication, inflating the audit trail. The re-record path was removed; a refusal now relies on the normal commit. Re-verified:

```
BDC-2026-4KDQ7M2P | PIN_MISMATCH  | 1 row
BDC-2026-4KDQ7M2P | NOT_CONFIRMED | 1 row
BDC-2026-6MPT5RG9 | SUCCESS       | 1 row
BDC-2026-9XR3B8VT | SUCCESS       | 1 row
```

### Transaction behaviour confirmed in the database

After the run, four verification attempts were logged (two successes, two failures) but **only one new donation row** existed — the one eligible donor. Every refused attempt left an audit record and no donation, which is precisely the required behaviour.

---

## 6. Packaging

```
[INFO] Building war: target\bdcms.war
[INFO] BUILD SUCCESS
```

---

## 6a. Administrator deletion, verified against the database

The claim that deleting a donor **keeps the audit trail** was checked directly
rather than assumed. A throwaway donor was registered, issued a card, and a
failed verification recorded against them, producing an audit row carrying their
`donor_id`. After deletion:

```
+--------+-------------------+----------+---------------+----------+---------------------+
| log_id | card_no           | donor_id | result        | staff_id | attempted_at        |
+--------+-------------------+----------+---------------+----------+---------------------+
|      4 | BDC-2026-JSXG77PT |     NULL | NOT_CONFIRMED |        1 | 2026-08-28 14:07:36 |
+--------+-------------------+----------+---------------+----------+---------------------+

donor_rows = 0   card_rows = 0   audit_rows_total = 4   (unchanged)
```

The row survived with `donor_id` set to null; the card number, outcome, staff
account and timestamp are all intact. The guard was checked too: deleting a
donor with donations is refused, and the donor still exists afterwards.

---

## 6b. Myanmar (Burmese) text

Checked end to end by registering a donor named **ကောင် မြတ်နိုး** through the real
web form.

| | |
|---|---|
| Stored in MySQL, read back byte-identical | ✅ |
| Displayed correctly in the application | ✅ |
| Rendered in generated PDFs | ❌ |

The web path is UTF-8 throughout (`utf8mb4` schema, `characterEncoding=UTF-8`,
`EncodingFilter`) and round-trips correctly. The PDFs do not: inspecting a
generated card shows only the built-in Type 1 fonts with `WinAnsiEncoding` and
no embedded font file —

```
/BaseFont  Courier-Bold, Helvetica, Helvetica-Bold
/Encoding  WinAnsiEncoding
/FontFile  none
```

WinAnsi is a single-byte Latin encoding, so Burmese cannot be represented in it
at all. Fixing this means embedding a Myanmar-capable TrueType font (Padauk or
Noto Sans Myanmar) in `CardPdfRenderer` and `PdfExporter`. **Not yet done.**

---

## 7. Not covered

Stated plainly rather than left implied.

- **Donor registration with a photo upload** was not exercised by the HTTP run — a multipart POST with a real image file. The duplicate-national-ID path and `PhotoStore` magic-byte checking are therefore covered only by the manual walkthrough in [SETUP.md §7](../SETUP.md).
- **Card issue and reissue** were verified by reading the seeded reissue trail, not by issuing a new card over HTTP.
- **PDF and QR rendering** are verified only as far as content type and non-trivial byte length; nobody has confirmed visually that the card looks right.
- **Concurrency.** The duplicate-registration race is handled by the `UNIQUE` index and the duplicate-key path in `DonorService`, but no automated concurrent test exercises it.
- **Session timeout** is configured at 20 minutes and not waited out.
- **The webcam scanner** cannot be driven from a script: it needs a camera, a
  permission grant and a physical card. The wiring is verified (the script and
  the jsQR fallback both serve, and the card field is present on both pages) but
  the decode path itself has been exercised only by hand.
- **The 3D card model** renders in a browser; only its asset graph is checked.
