# Blood Donation Card Management System

A centralised web-based donor card, identity verification and eligibility platform, built on Jakarta EE.

**Section D · Group 3**

> 👥 **New to the project? Start with [TEAM-GUIDE.md](TEAM-GUIDE.md)** — what every folder is for, how to set it up on your machine, and which folder you should be editing.

---

## The problem

Donation centres keep donor records in isolation. Three consequences follow, and this system addresses each one:

| Problem | How the system answers it |
|---|---|
| The same person registers twice and their history fragments | One record per person, enforced by a `UNIQUE` constraint on the national ID **at the database level** |
| No reliable way to confirm the person presenting a card owns it | A verification step — stored photograph, masked national ID, explicit staff confirmation, optional PIN — that **must** complete before a donation can be recorded |
| Staff cannot tell whether the mandatory interval has elapsed | Automatic eligibility checking against a configurable interval, computed from the donor's complete centralised history |

---

## Stack

Java · Jakarta EE 10 (Servlets 6.0, JSP 3.1, JSTL 3.0) · JDBC + DAO · HikariCP · MySQL 8.0 · Tomcat 10.1 · Maven · Bootstrap 5.3 · ZXing (QR) · jBCrypt · OpenPDF · JUnit 5

The interface follows a bespoke design system — Archivo and Roboto Mono, a bone/ink/crimson palette — built from the approved mockups. Bootstrap, the fonts and the QR decoder are all **bundled locally**, so the system runs with no internet connection.

Core Jakarta EE — Option A of the proposal — so servlets, JSP and JDBC are visible in the submitted code rather than hidden behind a framework's auto-configuration.

---

## Quick start

You need a **JDK** and **MySQL 8.0**. Maven and Tomcat are downloaded automatically.

```powershell
# 1. clone
git clone https://github.com/velluzHTA-dev/BloodDonation_Card_Management_System.git
cd BloodDonation_Card_Management_System

# 2. create and seed the database
mysql -u root -p < db/01_schema.sql
mysql -u root -p < db/02_seed.sql

# 3. set your MySQL host/port/password in src/main/resources/db.properties

# 4. build and run
.\mvnw.cmd clean package cargo:run
```

Open <http://localhost:8081/bdcms>. The sign-in page lists the demonstration
accounts with click-to-fill — or sign in as `admin` / `Admin@123`.

> **On this machine everything is already installed and verified.** MySQL 8.0.37 runs on port **3307** and the app on port **8081**. See the box at the top of **[SETUP.md](SETUP.md)** for the two commands that bring it back up after a reboot.

Full instructions, a guided walkthrough of every feature, and troubleshooting: **[SETUP.md](SETUP.md)**.

---

## Features

| ID | Module | What it does |
|---|---|---|
| FR-1 | Authentication | Staff and administrator login, BCrypt-hashed passwords, role-based access |
| FR-2 | Donor Registration | Demographics, blood group, photograph, unique national ID; a repeat ID is rejected **and the existing record shown** |
| FR-3 | Card Management | Unique card number, QR code, printable ID-1 card as PDF, reissue and deactivation |
| FR-4 | Identity Verification | Scan or type the card number, display the photograph and masked ID, require explicit confirmation, optional 4-digit PIN, every attempt audited |
| FR-5 | Eligibility Check | Days since last donation against a configurable minimum; returns ELIGIBLE, or NOT ELIGIBLE with the next permitted date |
| FR-6 | Donation Records | Appended only after verification and eligibility both pass, in a single transaction; full chronological history per donor |
| FR-7 | Reporting | Donations by date range, blood-group distribution, donor activity — each exportable to PDF |

Plus:

- a **shift dashboard** with counters, a scan bar and today's blood-group mix
- **webcam QR scanning** — the browser camera reads the card, on top of the handheld-scanner and type-it-in paths the proposal lists
- **assisted face check** — compares the live camera image with the stored photograph and reports a likeness. Strictly advisory: it cannot tick the confirmation, cannot block a donation, and is recorded in the audit trail rather than acted on
- **administrator data management** — delete a donor, guarded so history is never destroyed
- an administrator **settings** screen (the waiting period, edited without a rebuild), **staff account management**, and the **verification audit log**
- **English and Burmese throughout** — every screen, flash message, validation error, generated card and PDF report. A switcher in the top bar changes language in place, keeping you on the page and filter you were on

---

## How it is organised

The system is organised **by feature**, not by layer. Each feature is a vertical slice owning its servlets, service, DAO and model:

```
com.bdcms.
├── shared/          db · config · web · util      the only package every feature may use
└── feature/
    ├── auth/  donor/  card/  verification/
    ├── eligibility/  donation/  report/
    └── dashboard/  admin/
```

Two rules keep the dependency graph acyclic:

1. **Features talk through services, never DAOs.** A DAO only touches the tables its own feature owns.
2. **Where a code dependency would create a cycle, features are joined by a URL instead** — registration redirects to card issue; the verification form posts to the donation endpoint.

`donation` sits at the top and is the only feature that composes others, because it is the only one that has to: recording a donation means verification, eligibility and insertion succeeding or failing **together**, in one transaction.

The design, the reasoning behind it, and a full request trace: **[ARCHITECTURE.md](ARCHITECTURE.md)**.

---

## Verification

```powershell
.\mvnw.cmd -P jsp-check clean verify
```

- **106 unit tests**, all passing, none requiring a database
- **18 JSPs** translated by Jasper — a broken taglib or a stray scriptlet fails the build
- `target/bdcms.war`

Then, against a running instance with the database freshly seeded:

```powershell
.\scripts\smoke-test.ps1
```

**56 end-to-end checks, all passing** — login, CSRF, role enforcement, ID masking, PIN verification, the 84/83-day eligibility boundary, transactional recording, PDF export, the audit trail, and the guards on administrator deletion.

That live run caught two defects the build could not: Jakarta EL 5.0 cannot read Java **records** as bean properties, and failed verifications were being written to the audit log twice. Both are fixed and documented in **[docs/test-evidence.md](docs/test-evidence.md)**.

---

## Documentation

| Document | Contents |
|---|---|
| [USER-MANUAL.md](USER-MANUAL.md) | **For users** — how to operate the system, by role: staff, administrator, and what to tell donors |
| [TEAM-GUIDE.md](TEAM-GUIDE.md) | **For teammates** — folder structure, machine setup, who edits what, git workflow |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Feature map, dependency graph, request trace, the transaction, design decisions |
| [SETUP.md](SETUP.md) | Install, build, run, sign in, guided walkthrough, troubleshooting |
| [docs/er-diagram.md](docs/er-diagram.md) | Schema, constraints, indexes and the invariants enforced in code |
| [docs/test-evidence.md](docs/test-evidence.md) | Test results and the defects the build caught |
| [docs/card-3d/](docs/card-3d/) | The donor card as a 3D object — orbit it, export OBJ/GLB. A presentation asset, not part of the app |

---

## Notes for assessment

- **Two deviations from the proposal**, both deliberate and documented:
  - `donors.pin_hash` is added to the Section 9 schema. Section 10 specifies an optional four-digit PIN as a second factor but the schema had no column for it. It is BCrypt-hashed, never stored in plain text.
  - `maven.compiler.release` targets the installed JDK rather than 17. The code uses no API newer than 17; change the one property in `pom.xml` to build for 17.
- **Out of scope, as stated in the proposal:** blood inventory, public donor portal, external hospital integration, medical screening, native mobile apps. The web interface is responsive.
- Bootstrap, the Archivo/Roboto Mono fonts and the jsQR decoder are **bundled locally** under `src/main/webapp/assets/`, so the system runs with no internet connection.
- **Webcam scanning needs a secure context** — HTTPS, or the site opened on `localhost`. On localhost, where this is demonstrated, plain HTTP is fine. The button says so rather than failing silently, and typing the card number always works.
- **Myanmar (Burmese) text** is supported in the interface and in generated PDFs. Padauk (SIL Open Font License) is registered under the same CSS family names with a `unicode-range`, so Latin keeps Archivo and Burmese resolves to Padauk per character — no font stacks change, and the 78 KB is downloaded only when Myanmar text is actually on the page. The browser shapes Burmese correctly; the PDFs embed the same face, with the caveat below.
- **Myanmar text** stores and displays correctly end to end (`utf8mb4` throughout, verified), and now also renders in generated PDFs: Padauk (SIL Open Font License) is embedded with `Identity-H` for any string containing Myanmar characters, while Latin text stays on Helvetica. See `shared/pdf/PdfFonts`. One caveat is documented there — OpenPDF has no complex-text shaping engine, so glyphs are present and selectable but multi-part syllable stacking is not guaranteed.
