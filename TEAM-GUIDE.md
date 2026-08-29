# Team Guide

Blood Donation Card Management System · Section D, Group 3

Read this before you write any code. It covers three things: **what every folder is for**, **how to get the project running on your own machine**, and **which folder you should be editing** for the work you have been given.

---

## Part 1 — Folder structure

The **Edit?** column is the important one. If it says *No*, changing that folder will either be overwritten, break the build, or cause a merge conflict with everyone else.

```
BloodDonation_Card_Management_System/
│
├── src/main/java/com/bdcms/     ← ALL Java code. This is where you work.
│   ├── shared/                  ← Careful: everyone depends on this
│   └── feature/                 ← Your feature lives here
│
├── src/main/webapp/             ← Web pages (JSP), CSS, JavaScript, fonts
├── src/main/resources/          ← Config files
├── src/test/java/               ← Unit tests
├── db/                          ← Database scripts
├── docs/                        ← Diagrams, test results, the 3D card
├── scripts/                     ← smoke-test.ps1, the end-to-end suite
├── target/                      ← Build output. Never touch, never commit.
└── (root files)                 ← Build config and documentation
```

### In detail

| Folder / file | What it holds | Edit? |
|---|---|---|
| `src/main/java/com/bdcms/feature/<name>/` | **Your feature.** Servlets, service, DAO, model — one complete slice | **Yes** — this is your main workspace |
| `src/main/java/com/bdcms/shared/` | Database pool, transactions, filters, base servlet, validation | **Ask first** — every feature breaks if this breaks |
| `src/main/webapp/WEB-INF/views/<feature>/` | The JSP pages for your feature | **Yes** |
| `src/main/webapp/WEB-INF/views/layout/` | Shared header, footer, taglibs | **Ask first** — appears on every page |
| `src/main/webapp/WEB-INF/web.xml` | Filters, session timeout, error pages | **Ask first** |
| `src/main/webapp/assets/css/bdcms.css` | Our own styles | **Yes**, add at the bottom |
| `src/main/webapp/assets/js/bdcms.js` | Our own JavaScript | **Yes** |
| `src/main/webapp/assets/js/bdcms-scanner.js` | Webcam QR scanning for the card field | **Yes** |
| `src/main/webapp/assets/fonts/` | Archivo + Roboto Mono, self-hosted | **No** — third-party |
| `src/main/resources/fonts/` | Padauk, embedded into generated PDFs | **No** — third-party |
| `src/main/webapp/assets/vendor/` | Bootstrap 5.3 and jsQR, downloaded | **No** — third-party |
| `src/main/resources/db.properties` | Database host, port, password | **Yes, locally** — but see the warning in Part 2 |
| `src/main/resources/app.properties` | Upload folder, photo size limit, BCrypt cost | Rarely |
| `src/test/java/com/bdcms/` | JUnit tests, mirroring the main package layout | **Yes** — add tests for your feature |
| `db/01_schema.sql` | Table definitions | **Ask first** — a schema change affects everyone |
| `db/02_seed.sql` | Demo data: 3 staff, 5 donors, 6 cards, 7 donations | **Ask first** |
| `docs/` | ER diagram, test evidence | Yes, when you have something to record |
| `docs/card-3d/` | The card as a 3D object. A presentation asset, not part of the app | Rarely |
| `scripts/smoke-test.ps1` | 56 end-to-end checks against a running instance | **Yes** — add a check when you add a feature |
| `pom.xml` | Dependencies and build plugins | **Ask first** |
| `mvnw`, `mvnw.cmd`, `.mvn/` | Maven wrapper — downloads Maven for you | **No** |
| `target/` | Compiled classes and the WAR | **No** — auto-generated, excluded by `.gitignore` |
| `README.md`, `ARCHITECTURE.md`, `SETUP.md`, `USER-MANUAL.md` | Project documentation | Yes, keep them true |
| `.gitattributes` | Line-ending rules so Windows and macOS agree | **No** |

### Inside one feature — the pattern

Every feature package has the same five kinds of file. Learn this once and you can find your way around any of them:

```
com/bdcms/feature/donor/
├── DonorListServlet.java      CONTROLLER  reads the request, calls the service, picks a JSP
├── DonorRegisterServlet.java  CONTROLLER
├── DonorService.java          SERVICE     the business rules live here
├── DonorDao.java              DAO         the only place SQL for the donors table is written
├── Donor.java                 MODEL       plain data object
└── BloodGroup.java            MODEL       enum
```

**The flow is always the same:**

```
Browser  →  Servlet  →  Service  →  DAO  →  MySQL
                                            ↓
Browser  ←   JSP    ←──────────────────────
```

Three rules that follow from it, and that reviewers will check:

1. **No SQL outside a DAO.** Not in a servlet, not in a service.
2. **No business logic in a JSP.** The build enforces this — a `<% ... %>` scriptlet makes compilation *fail*.
3. **Never call another feature's DAO.** Call its *service* instead. `CardServlet` needs donor details, so it calls `donorService.findById(...)`, never `DonorDao`.

---

## Part 2 — Setting up on your machine

You need a **JDK 17 or newer** and **MySQL 8.0**. You do *not* need to install Maven or Tomcat — the project downloads them itself.

### 1. Clone

```powershell
git clone https://github.com/velluzHTA-dev/BloodDonation_Card_Management_System.git
cd BloodDonation_Card_Management_System
```

### 2. Create the database

With MySQL 8 installed and running:

```powershell
mysql -u root -p < db/01_schema.sql
mysql -u root -p < db/02_seed.sql
```

`01_schema.sql` begins with `DROP DATABASE IF EXISTS bdcms`, so re-running these two is how you reset to a clean state whenever your data gets messy.

### 3. Point the app at *your* database

Open `src/main/resources/db.properties` and set the port and password to match **your** MySQL:

```properties
bdcms.db.url=jdbc:mysql://127.0.0.1:3306/bdcms?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
bdcms.db.username=root
bdcms.db.password=YOUR_PASSWORD
```

> ⚠️ **The file in git is set up for Hein Thu's machine — port 3307, password `bdcms`.** Yours will differ.
>
> **Do not commit your version.** It will overwrite everyone else's settings and they will all get `Communications link failure`. Tell git to ignore your local edits:
>
> ```powershell
> git update-index --skip-worktree src/main/resources/db.properties
> ```
>
> Run that once, after cloning. To undo it later: `git update-index --no-skip-worktree src/main/resources/db.properties`

### 4. Run it

```powershell
.\mvnw.cmd clean package cargo:run
```

First run takes a few minutes — it downloads Maven, Tomcat 10.1 and the libraries. After that it starts in seconds.

Open **<http://localhost:8081/bdcms>**

If port 8081 is taken on your machine: `.\mvnw.cmd cargo:run "-Dcargo.port=9090"`

### 5. Sign in

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | Administrator — sees Reports, Settings, Audit Log |
| `staff` | `Staff@123` | Staff — everyday counter work |

The sign-in page lists these with click-to-fill, so you need not retype them.
Every demo donor's verification PIN is **`1234`**.

### 6. Check your setup works

```powershell
.\mvnw.cmd test
```

You should see **`Tests run: 106, Failures: 0, Errors: 0`**. If that passes, your JDK and the project are fine even before MySQL is working.

### 7. Check the whole system works

With the app running and the database freshly seeded:

```powershell
.\scripts\smoke-test.ps1
```

**56 checks** covering login, CSRF, roles, ID masking, PIN, the eligibility
boundary, PDF export, the audit trail and administrator deletion. Reseed before
each run — the suite records a donation and deletes a donor, so a second run
without reseeding reports failures that are the system behaving correctly.

---

## Part 3 — Who edits what

The whole point of organising by feature is that **six people can work at once without touching the same files.** Stay inside your own folder and you will rarely hit a merge conflict.

Fill in the names — the assignment is a suggestion, swap as you like.

| Person | Feature | Java folder | JSP folder |
|---|---|---|---|
| Hein Thu | **Shared + integration**<br>plus `dashboard/` and `admin/` | `shared/`<br>`feature/dashboard/`, `feature/admin/` | `views/layout/`<br>`views/dashboard/`, `views/admin/` |
| *(htarinzali76@)* | **FR-1 Authentication**<br>**FR-7 Reporting** | `feature/auth/`<br>`feature/report/` | `views/auth/`<br>`views/report/` |
| *(shunnlae1355@)* | **FR-2 Donor registration** | `feature/donor/` | `views/donor/` |
| *(eoo583226@)* | **FR-3 Card management** | `feature/card/` | `views/card/` |
| *(ayechanpyaemon3@)* | **FR-4 Identity verification** | `feature/verification/` | `views/verification/` |
| *(pwintwaiwaitha@)* | **FR-5 Eligibility**<br>**FR-6 Donation records** | `feature/eligibility/`<br>`feature/donation/` | `views/donation/` |

### Which feature depends on which

This matters for ordering your work. An arrow means *"needs this to be working first."*

```
                 report
                    │
                 donation          ← finish last: it uses three others
        ┌───────────┼───────────┐
        ▼           ▼           ▼
  verification  eligibility   card
        │                       │
        └────────► donor ◄──────┘
                     │
                  shared          ← finish first: everything sits on it
```

So: **shared and donor first**, then card and verification, then donation, then report.

`eligibility` is independent — it depends only on `shared`, so whoever has it can start immediately.

### "I want to change X — where do I go?"

| I want to... | Edit |
|---|---|
| Change what the donor registration form asks for | `views/donor/register.jsp` + `DonorRegisterServlet` + `DonorDao` + `db/01_schema.sql` |
| Change a validation rule (phone, age, ID format) | `shared/util/Validate.java` |
| Change the donation waiting period | **No code.** Sign in as admin → Administration → System Settings |
| Change how the printed card looks | `card/CardPdfRenderer.java` (the PDF) and `views/card/print.jsp` (the preview) |
| Change how much of the national ID is hidden | `verification/IdMasker.java` |
| Change the webcam scanner | `assets/js/bdcms-scanner.js` |
| Change the sidebar or top bar | `views/layout/header.jspf` |
| Add an icon | `views/layout/icons.jspf`, then `<use href="#i-yourname"/>` |
| Change how PDFs pick fonts | `shared/pdf/PdfFonts.java` |
| Change what an administrator may delete | `feature/admin/DataAdminService.java` |
| Add a column to a report | `report/ReportDao.java` (the SQL) and `report/ReportService.java` (the headers) |
| Change colours, spacing, layout | `assets/css/bdcms.css` |
| Add a menu item | `views/layout/header.jspf` — **tell the team, it affects every page** |
| Add a new page to your feature | New servlet in your package + new JSP in your `views/<feature>/` folder |

### Adding a brand-new feature

Copy the pattern, do not invent a new one:

1. `src/main/java/com/bdcms/feature/yourfeature/`
2. Inside it: `YourThing.java` (model), `YourThingDao.java` (SQL only), `YourThingService.java` (rules), `YourThingServlet.java` (controller)
3. Map the servlet under `/app/...` so `AuthFilter` protects it: `@WebServlet("/app/yourthing")`
4. Extend `BaseServlet` — you get `render()`, `redirect()`, `currentUser()` and parameter helpers free
5. JSPs go in `src/main/webapp/WEB-INF/views/yourfeature/`
6. Tests go in `src/test/java/com/bdcms/feature/yourfeature/`

---

## Part 3a — ⚠️ Read this before you push existing work

**The interface was rebuilt.** If your branch was cut before that, you will hit
conflicts. Pull and rebase *before* you write any more code:

```powershell
git checkout main
git pull
git checkout your-branch
git merge main
```

Expect conflicts in these, because they were rewritten rather than edited:

| File | What changed |
|---|---|
| `assets/css/bdcms.css` | Rewritten. New palette, sidebar shell, new component styles |
| `views/layout/header.jspf` | Rewritten. Top navbar became a left sidebar plus a top bar |
| `views/layout/footer.jspf` | Rewritten to close the new shell |
| `views/dashboard/dashboard.jsp` | Rewritten to the new mockup |
| `views/layout/icons.jspf` | **New.** SVG icon sprite |

**Resolving them:** take *main's* version of these five files, then re-apply your
own change on top. Do not merge them line by line — the two versions are
different designs, and interleaving them produces neither.

Class names did **not** change, so if your work was in another view it will
almost certainly still render correctly. What you may want to add:

- a `topTitle` / `topSub` request attribute for the top bar heading
- `<svg class="ic"><use href="#i-users"/></svg>` for an icon (see `icons.jspf`)

If your branch adds a **new page**, copy the shell usage from any current view:
set `pageTitle`, `nav`, and optionally `topTitle` / `topSub`, then include
`header.jspf` and `footer.jspf` as before.

---

## Part 4 — Working together in git

**Never commit straight to `main`.** Work on a branch named after your feature:

```powershell
git checkout main
git pull                              # always start from the latest

git checkout -b feature/card-expiry   # your branch
# ... edit files ...

.\mvnw.cmd test                       # must pass before you push
git add -A
git commit -m "Add expiry date to donor cards"
git push -u origin feature/card-expiry
```

Then open a **Pull Request** on GitHub and ask someone to review it.

### Before every push, check

- [ ] `.\mvnw.cmd test` passes — 106 tests, zero failures
- [ ] `.\mvnw.cmd -P jsp-check verify` passes if you touched any JSP
- [ ] You did **not** commit `src/main/resources/db.properties`
- [ ] You did **not** commit `target/` (`.gitignore` handles this — don't override it)
- [ ] You stayed inside your own feature folder, or warned the team

### If you get a merge conflict

```powershell
git checkout main
git pull
git checkout your-branch
git merge main
# fix the conflicted files, then:
git add .
git commit
```

Conflicts in `shared/`, `layout/`, `web.xml`, `pom.xml` or `db/` mean two people changed something communal. **Talk to each other before resolving** — silently picking one side is how work gets lost.

---

## Part 5 — Things that will trip you up

| Symptom | Cause and fix |
|---|---|
| `Communications link failure` | MySQL isn't running, or `db.properties` has the wrong port/password |
| `Unknown database 'bdcms'` | You skipped `db/01_schema.sql` |
| Can't sign in with `admin` / `Admin@123` | You skipped `db/02_seed.sql` |
| Every page shows **500**, but PDFs work | A JSP is reading a Java **record** as a property. Tomcat 10.1's EL can't do that — add ordinary `getX()` methods to the record |
| Build fails: *"Scripting elements are disallowed"* | You put Java code in a JSP. Move it into the servlet or service — this rule is enforced deliberately |
| `clean` fails: *"Failed to delete ...cargo..."* | The app is still running. Stop it with `Ctrl+C` first |
| `PKIX path building failed` | Antivirus is inspecting HTTPS. `mvnw.cmd` already works around it; if you run plain `mvn`, set `$env:MAVEN_OPTS = '-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT'` |
| Port 8081 already in use | `.\mvnw.cmd cargo:run "-Dcargo.port=9090"` |
| My teammate's changes broke my feature | They probably edited `shared/`. `git pull` and re-run the tests |

---

## Where to read next

| Document | Read it when |
|---|---|
| [README.md](README.md) | You want the one-page overview |
| [SETUP.md](SETUP.md) | Setup fails, or you want the 14-step feature walkthrough |
| [ARCHITECTURE.md](ARCHITECTURE.md) | You want to understand *why* it is built this way — required reading before touching `shared/` or the donation transaction |
| [docs/er-diagram.md](docs/er-diagram.md) | You are writing SQL or changing a table |
| [docs/test-evidence.md](docs/test-evidence.md) | You are writing the report, or want to know what is tested |

**When in doubt: ask in the group chat before editing anything outside your own feature folder.**
