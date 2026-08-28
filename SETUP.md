# Setup and run guide

Blood Donation Card Management System · Section D, Group 3

You need **a JDK (17 or newer)** and **MySQL 8.0**. You do *not* need Maven or Tomcat installed — the wrapper fetches Maven, and the Cargo plugin fetches Tomcat 10.1.

---

> ## Already installed and running on this machine
>
> MySQL 8.0.37 has been installed and the application has been verified end to end. To bring it back up after a reboot:
>
> ```powershell
> # 1. start MySQL (no Windows service - it runs as a plain process)
> Start-Process 'C:\Users\heint\mysql8\mysql-8.0.37-winx64\bin\mysqld.exe' `
>               -ArgumentList '--defaults-file=C:\Users\heint\mysql8\my.ini' -WindowStyle Hidden
>
> # 2. start the application
> cd c:\Users\heint\BloodDonation_Card_Management_System
> .\mvnw.cmd cargo:run
> ```
>
> Then open **<http://localhost:8081/bdcms>** and sign in as `admin` / `Admin@123`.
>
> | Setting | Value |
> |---|---|
> | MySQL location | `C:\Users\heint\mysql8\mysql-8.0.37-winx64` (ZIP install, no admin rights needed) |
> | MySQL data / config | `C:\Users\heint\mysql8\data` · `C:\Users\heint\mysql8\my.ini` |
> | MySQL port | **3307** — deliberately not 3306, so it cannot clash with the MySQL 5.6 already installed |
> | MySQL root password | `bdcms` |
> | Application port | **8081** — 8080 is occupied by another process (`AgentService`) on this machine |
>
> To stop MySQL: `Get-Process mysqld | Stop-Process`. To stop the app: `Ctrl+C` in its window.
>
> The rest of this section describes installing from scratch on a different machine.

---

## 1. Install MySQL 8.0

This machine also has **MySQL Server 5.6**, which is end-of-life and not running; it was left untouched.

Two ways to install 8.0:

**ZIP archive (no administrator rights)** — what was used here. Download `mysql-8.0.x-winx64.zip`, extract it, write a `my.ini` pointing `basedir`/`datadir`/`port` at your chosen locations, then:

```powershell
& "<extracted>\bin\mysqld.exe" --defaults-file=<your my.ini> --initialize-insecure --console
Start-Process "<extracted>\bin\mysqld.exe" -ArgumentList '--defaults-file=<your my.ini>' -WindowStyle Hidden
& "<extracted>\bin\mysql.exe" -h 127.0.0.1 -P <port> -u root --skip-password `
    -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '<password>';"
```

**MySQL Installer (needs administrator rights)** — the classic route, which also registers a Windows service:

1. Open **MySQL Installer for Windows** (already present under `C:\Program Files (x86)\MySQL\MySQL Installer for Windows`), or download it from <https://dev.mysql.com/downloads/installer/>.
2. Choose **MySQL Server 8.0**. Accept the default port **3306**.
3. Set a root password and remember it.
4. Let the installer configure MySQL as a **Windows service** so it starts automatically.

Check that it is running:

```powershell
Get-Service | Where-Object { $_.DisplayName -match 'MySQL' }
Test-NetConnection -ComputerName 127.0.0.1 -Port 3306 -InformationLevel Quiet   # must print True
```

If the service exists but is stopped:

```powershell
Start-Service MySQL80
```

---

## 2. Create the database

From the project root, using the MySQL 8.0 client (adjust the path to match your install):

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

& $mysql -u root -p --default-character-set=utf8mb4 -e "source db/01_schema.sql"
& $mysql -u root -p --default-character-set=utf8mb4 -e "source db/02_seed.sql"
```

Or open both files in **MySQL Workbench 8.0** (already installed) and run them in order — `01_schema.sql` first.

`01_schema.sql` starts with `DROP DATABASE IF EXISTS bdcms`, so re-running it is a clean reset.

Confirm:

```sql
USE bdcms;
SELECT COUNT(*) FROM donors;      -- 5
SELECT COUNT(*) FROM donations;   -- 7
SELECT * FROM settings;           -- min_donation_interval_days = 84
```

---

## 3. Point the application at your database

Edit `src/main/resources/db.properties` and set the password you chose:

```properties
bdcms.db.url=jdbc:mysql://localhost:3306/bdcms?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
bdcms.db.username=root
bdcms.db.password=YOUR_PASSWORD_HERE
```

Every value can also be overridden at runtime without rebuilding:

```powershell
mvnw.cmd cargo:run "-Dbdcms.db.password=secret"
```

---

## 4. Build

```powershell
cd c:\Users\heint\BloodDonation_Card_Management_System

.\mvnw.cmd clean test        # 106 unit tests, no database needed
.\mvnw.cmd clean package     # produces target\bdcms.war
```

The first run downloads Maven 3.9.9 and the dependencies; later runs are fast.

To also put every JSP through Jasper (catches broken taglibs and any scriptlet):

```powershell
.\mvnw.cmd -P jsp-check clean verify
```

Once it is running, the end-to-end suite exercises the whole system over HTTP:

```powershell
.\scripts\smoke-test.ps1
```

**53 checks.** Reseed the database first — the suite records a donation and
deletes a donor, so a second run without reseeding reports failures that are
the system working correctly.

---

## 5. Run

### Option A — embedded Tomcat, nothing to install

```powershell
.\mvnw.cmd clean package cargo:run
```

Cargo downloads Tomcat 10.1.30 on first use, deploys the WAR and starts it.

Open <http://localhost:8081/bdcms> · stop with `Ctrl+C`.

> The port is a Maven property (`cargo.port`, default 8081). Override it with
> `.\mvnw.cmd cargo:run "-Dcargo.port=9090"` if 8081 is taken.

### Option B — your own Tomcat 10.1

```powershell
Copy-Item target\bdcms.war "C:\apache-tomcat-10.1.x\webapps\"
C:\apache-tomcat-10.1.x\bin\startup.bat
```

> Tomcat **10.1 or newer** is required. The application uses the `jakarta.*` namespace; Tomcat 9 and earlier use `javax.*` and will not run it.

### Option C — Eclipse

`File → Import → Existing Maven Projects`, select the project root, then `Run As → Run on Server` with a Tomcat 10.1 runtime.

---

## 6. Sign in

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | Administrator |
| `staff` | `Staff@123` | Staff |
| `nurse` | `Nurse@123` | Staff |

Every seeded demo donor has the verification **PIN `1234`**.

> Change these before any real use. Passwords are stored as BCrypt hashes; an administrator can reset them at **Administration → Staff Accounts**.

---

## 7. Walk through the whole system

The seed data is arranged so every branch can be demonstrated immediately.

**1. Dashboard** — sign in as `admin`. Donor counts, active cards, donations this month, failed verifications.

**2. Register a donor** — *Register Donor*. Fill the form, attach a JPEG or PNG photo, optionally set a 4-digit PIN. On success you are taken straight to card issue.

**3. Duplicate rejection (FR-2)** — register again with national ID `S1234567A`. It is rejected and **Hein Thu's existing record is shown**, with links to open it.

**4. Issue and print a card (FR-3)** — issue the card, then *Open printable PDF*. Name, blood group, photograph, card number and QR code, sized to a real ID-1 card.

**5. Verification (FR-4)** — *Verify & Donate*, enter `BDC-2026-6MPT5RG9` (Min Khant Zaw). The photograph appears large with the masked ID `S••••901E` beneath it. A PIN box appears because this donor has one.

**6. Record a donation (FR-6)** — tick the confirmation, enter PIN `1234`, submit. Min Khant Zaw has never donated, so he is eligible and the donation is recorded.

**7. Eligibility refusal (FR-5)** — try `BDC-2026-9XR3B8VT` (Su Su Aung, donated 30 days ago). Identity verifies, but the donation is refused with the next permitted date and the days remaining.

**8. The boundary** — `BDC-2026-7QJ2FE5S` (Kyaw Zin Latt) last donated exactly 84 days ago and **is** eligible. Thida Win, at 83 days, is not.

**9. Inactive card** — try `BDC-2026-1HN6CY4W`. It was reported lost and is refused, with a link to issue a replacement. Kyaw Zin Latt's history is intact because it hangs off the donor, not the card.

**10. Reissue** — on any donor's *Cards* page, *Report lost*, then reissue. The old card becomes LOST, the new one ACTIVE, history untouched.

**11. Reports (FR-7)** — *Administration → Reports*. Three reports, each with **Export to PDF**.

**12. The audit trail** — *Administration → Verification Audit Log*. Every attempt from steps 5–9 is there, successes and failures, each with the staff account and timestamp:

```sql
SELECT card_no, result, staff_id, attempted_at FROM bdcms.verification_log ORDER BY attempted_at DESC;
```

**13. Configurable rule** — *Administration → System Settings*. Change `min_donation_interval_days` from 84 to 20, then retry step 7: Su Su Aung is now eligible. No rebuild, no restart. Set it back to 84.

**14. Session timeout** — leave the browser idle 20 minutes; the next action returns you to the login page with your destination preserved.

---

## 8. Troubleshooting

**`PKIX path building failed` during a Maven build**
Antivirus or a proxy is inspecting TLS, and its certificate is in the Windows store but not in the JDK's. `mvnw.cmd` already sets `-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT` to handle this. If you run `mvn` directly, set it yourself:
```powershell
$env:MAVEN_OPTS = '-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT'
```

**`Communications link failure` on startup**
MySQL is not running. `Start-Service MySQL80`, and confirm port 3306 with `Test-NetConnection`.

**`Public Key Retrieval is not allowed`**
The URL in `db.properties` already includes `allowPublicKeyRetrieval=true`; make sure you did not remove it.

**`Unknown database 'bdcms'`**
`db/01_schema.sql` has not been run — see §2.

**Blank page or `ClassNotFoundException: javax.servlet.*`**
You are on Tomcat 9 or earlier. This application needs Tomcat 10.1+.

**Port 8081 already in use**
The embedded Tomcat port is a property. Override it:
```powershell
.\mvnw.cmd cargo:run "-Dcargo.port=9090"
```
On this machine the default is already 8081 because 8080 is taken.

**`Failed to clean project: Failed to delete ...\cargo\installs\...jar`**
The application is still running and holding those files. Stop it with `Ctrl+C` (or `Get-NetTCPConnection -LocalPort 8081 -State Listen` then `Stop-Process`) before running `clean`. Plain `mvnw.cmd verify` works while it is running.

**Every page returns 500 but PDFs and images work**
A JSP is reading a Java **record** as a bean property. Tomcat 10.1 implements Jakarta EL 5.0, which cannot do that — record support arrived in EL 6.0. Add ordinary `getX()` methods to the record; the five records used in views already have them.

**The webcam scan button says the camera needs HTTPS**
`getUserMedia` only works in a secure context: HTTPS, or the site opened on
`localhost`. On localhost — how this is normally run — plain HTTP is fine. If
you reach the app by IP address from another machine over HTTP, the camera will
not open; type the card number, or put the app behind HTTPS.

**Photographs do not appear**
They are written to `%USERPROFILE%\bdcms-uploads`. The directory is created at startup; check the Tomcat log line `BDCMS upload directory: ...`. Donors with no photo deliberately show a grey placeholder rather than a broken image.

**Cannot sign in with the seeded passwords**
Confirm `db/02_seed.sql` ran. `PasswordsTest.seededAdminHashIsValid` asserts that the hash in that file really does match `Admin@123`, so if the tests pass the seed is correct.

---

## 9. Project layout

```
├── db/                 01_schema.sql · 02_seed.sql
├── docs/               er-diagram.md · test-evidence.md · card-3d/
├── scripts/            smoke-test.ps1 — 53 end-to-end checks
├── src/main/java/      shared/ and feature/    (see ARCHITECTURE.md)
├── src/main/resources/ db.properties · app.properties
├── src/main/webapp/    WEB-INF/views/*  ·  assets/ (css, js, fonts, vendor)
├── src/test/java/      106 unit tests, no database required
├── ARCHITECTURE.md     how it is built and how the parts connect
├── TEAM-GUIDE.md       for teammates: folder map, setup, who edits what
├── USER-MANUAL.md      how to operate the system, by role
└── pom.xml
```
