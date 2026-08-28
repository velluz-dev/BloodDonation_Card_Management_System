# User Manual

Blood Donation Card Management System · Section D, Group 3

This manual covers every role that touches the system. Find your role, read that
part; the reference tables at the end apply to everyone.

| I am… | Read |
|---|---|
| A nurse or counter clerk recording donations | [Part 2 — Staff](#part-2--staff) |
| A supervisor running reports and managing accounts | [Part 2](#part-2--staff) **and** [Part 3 — Administrator](#part-3--administrator) |
| A donor, or explaining the system to one | [Part 4 — The donor](#part-4--the-donor) |

---

## Part 1 — Everyone

### The two roles

There are exactly two roles. An administrator can do everything a staff member
can, plus five things a staff member cannot.

| Screen | Staff | Administrator |
|---|:---:|:---:|
| Dashboard | ✅ | ✅ |
| Register a donor | ✅ | ✅ |
| Search and view donors | ✅ | ✅ |
| Issue, reissue and print cards | ✅ | ✅ |
| Verify a donor and record a donation | ✅ | ✅ |
| View donation history | ✅ | ✅ |
| **Reports and PDF export** | ❌ | ✅ |
| **Verification audit log** | ❌ | ✅ |
| **Staff accounts** | ❌ | ✅ |
| **System settings** | ❌ | ✅ |
| **Delete a donor record** | ❌ | ✅ |

A staff member who types an administrator URL directly gets **403 — you do not
have permission for this page**. The restriction is enforced on the server, not
by hiding the menu.

### Signing in

1. Open <http://localhost:8081/bdcms>
2. Enter your username and password
3. Press **Sign in**

If you were sent to the login page by clicking a link, you return to that page
after signing in rather than to the dashboard.

**Demonstration accounts.** The sign-in page lists these and fills the form when
you click one, so there is nothing to remember during a demonstration:

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | Administrator |
| `staff` | `Staff@123` | Staff |
| `nurse` | `Nurse@123` | Staff |

> That panel exists only because every account here is seeded and every donor
> invented. An administrator turns it off for real use by setting
> `bdcms.demo.credentials=false` — see [SETUP.md](SETUP.md).

If the password is wrong you get *"Incorrect username or password."* The message
is deliberately identical whether the username exists or not, and whether the
account is disabled or not — telling you which would tell an attacker too.

### Your session ends after 20 minutes idle

You are returned to the login page with *"Your session has ended."* Nothing you
had already saved is lost — but anything typed into a form and not submitted is.
On a shared counter machine, **sign out** when you step away rather than relying
on the timeout.

### Reading the screen

- **Crimson** means one of three things: a blood group, the main action on the
  page, or a refusal. It is never decoration — if something is crimson, it matters.
- **Green** means one thing only: the donor may donate.
- **Monospaced text** is a machine-read value — a card number, a national ID, a
  date, a volume. If it looks like typewriter text, it is meant to be compared
  character by character.
- The **role chip** in the top-right shows who you are signed in as. Filled
  crimson = administrator, outlined = staff.

---

## Part 2 — Staff

### 2.1 The dashboard

Your shift starts here. It shows registered donors, active cards, donations this
month, and verification attempts over the last seven days with failures called
out in crimson.

The **cursor is already in the scan field** when the page loads. A handheld
scanner types the card number and presses Enter by itself, so scanning a card
takes you straight to verification with no clicking.

### 2.2 Register a new donor

**Donors → Register donor**

1. Enter the **national ID**. This is the one field that cannot be changed later —
   it is what keeps a donor to a single record across every centre.
2. Fill in name, date of birth, gender, blood group and contact number.
3. Attach a **photograph** — JPEG or PNG, up to 2 MB. Front-on, face uncovered,
   plain background. This is the image you will compare against the person at
   every future donation, so a poor photo causes trouble for years.
4. Optionally set a **four-digit PIN** the donor chooses. It is asked for as a
   second check on return visits.
5. Press **Register donor**.

You are taken straight to card issue.

> **If the national ID is already registered**, the form is rejected and the
> existing donor is shown to you with their photo, blood group and donation
> count. This is not an error — it is the answer. Open that record; if they have
> lost their card, reissue it rather than creating a second donor.

**Age must be between 16 and 70.** Contact numbers must look like phone numbers.
Every field is checked again on the server, so a browser that skips validation
does not get past it.

### 2.3 Issue and print a card

**From a donor record → Cards → Issue card**

1. Check the donor's details on the confirmation screen.
2. If they already hold a card, choose why you are replacing it:
   - **Lost or stolen** — the old card is marked LOST
   - **Routine replacement** (damaged, worn) — marked INACTIVE
3. Press **Issue card**. A new unique number is generated with a QR code.
4. On the preview screen, press **Open printable PDF** and print at
   85.6 × 54 mm on card stock.

> Issuing a new card **immediately stops the old one working**. The donor's
> donation history is attached to the donor, not the card, so nothing is lost.

The printed card carries the donor's name, blood group, photograph, card number
and QR code. It does **not** carry the national ID, and the QR encodes only the
card number — a card dropped in the street should not hand a finder an identity.

### 2.4 Verify a donor and record a donation

This is the core task. **Verify & record**

**Step 1 — Scan.** Three ways, all equivalent:

- **Handheld scanner** — it types the number and presses Enter by itself.
- **Webcam** — press **Scan with camera**, hold the card's QR code up to the
  camera, and the number fills in and submits on its own. The first use asks
  permission for the camera; allow it once and the browser remembers.
- **Type it** — the field is always there, and always works.

> The camera needs HTTPS or the site opened on `localhost`. If neither applies,
> the button says so instead of failing silently — type the number instead.

**Step 2 — Compare.** The stored photograph fills the dark panel on the left.
Beside it sits the **masked national ID**, for example `S••••567A`.

- Look at the person. Look at the photograph.
- Ask for their identity document and check the visible characters of the ID
  against it.
- Check the **card status** reads Active.

**Step 3 — Confirm.** Tick the confirmation box. Read it before you tick it: you
are recording that you personally checked, and your account is attached to that
statement permanently.

If the donor set a PIN, a PIN box appears. Ask them to enter it themselves.

**Step 4 — Record.** Enter centre, volume (100–700 ml) and date, then press
**Confirm identity & record donation**.

Eligibility is checked at this point, not before. If the donor is inside the
waiting period, nothing is recorded and you are told the earliest date they may
return.

> **Every attempt is logged either way.** Refusals are recorded exactly like
> approvals, with your account and the time. This protects you as much as the
> donor: it is the evidence that you followed the procedure.

### 2.5 When it will not go through

| What you see | What it means | What to do |
|---|---|---|
| *No card matches that number* | Typo, or a card from another system | Retype it. Scanners occasionally drop a character |
| *This card is no longer active* | Reported lost, or superseded | Issue a replacement, then start again |
| *You must confirm the photograph and masked ID…* | The tick box was not ticked | Do the comparison, then tick it |
| *The PIN entered does not match* | Wrong PIN | Let them try again. If they have forgotten it, an administrator can clear it from the donor record |
| *Only N of the required 84 days have passed…* | Too soon since the last donation | Tell them the date shown. Do not attempt to work around it — the interval protects their health |
| **The photograph does not match the person** | Possible card misuse | **Do not confirm.** Leave the box unticked and submit; the refusal is logged. Escalate to your supervisor |

### 2.6 Look someone up

**Donors** — search by name, national ID or contact number, and filter by blood
group. From a donor you can reach their **cards**, their **donation history**,
and their **current eligibility** with the next permitted date.

### 2.7 A card has been reported lost

1. Open the donor → **Cards**
2. Press **Report lost** on the active card — it stops working immediately
3. Press **Reissue card** and print the new one

The old card stays in the list as a record. History is untouched.

---

## Part 3 — Administrator

Everything in Part 2, plus the following. All four live under **Reports** in the
navigation bar.

### 3.1 Reports and PDF export

**Reports**

| Report | Shows |
|---|---|
| **Donations by date range** | Every donation in a period with donor, card, centre, volume and the staff member who verified it |
| **Blood group distribution** | Donors and donations per ABO/Rh group |
| **Donor activity** | Per donor: donation count, last donation, current eligibility, next eligible date |

Set a date range where offered, then **Export PDF**. The PDF is built from the
same rows shown on screen, so the export can never disagree with the page — and
it is stamped with your name and the time it was generated.

### 3.2 Verification audit log

**Reports → Verification audit log**

Every verification attempt ever made: time, card number, donor, outcome, detail
and the staff account responsible. Failed attempts are tinted so they stand out.
Filter by date range or donor ID. The outcome tally on the right covers the last
30 days unless you filter.

Watch for: repeated **card not found** on similar numbers, any **photo mismatch**,
and **inactive card presented** more than occasionally.

This log is append-only. Rows cannot be edited or deleted by anyone, including
you — an audit trail that can be tidied up is not an audit trail.

### 3.3 Staff accounts

**Reports → Staff accounts**

- **Create** an account with a username, full name, role and initial password
  (8 characters minimum). Ask them to change it after their first sign-in.
- **Disable / Enable** an account. Disabling blocks sign-in immediately.
- **Reset password** if someone is locked out.

> Accounts are **disabled, never deleted**. Every donation and verification names
> the account responsible; deleting one would break the audit trail. You also
> cannot disable the account you are currently signed in with.

Passwords are stored as BCrypt hashes. Nobody — including you — can read an
existing password. It can only be replaced.

### 3.4 Delete a donor record

Open the donor → **Delete donor** on the left, under their photograph.

**When it is allowed.** Only while the donor has **no recorded donations**.
Donation history is append-only because the eligibility rule is computed from
it — a donation that happened must stay recorded. If they have donated, the
deletion is refused and the count shown. Deactivate their card instead if they
should no longer donate.

**What is removed:** the donor record, every card they hold, and their
photograph.

**What is kept:** the **verification audit log**. Those rows are not deleted —
their link to the donor is cleared. Each keeps its card number, outcome, staff
account and timestamp, so what happened at the counter remains auditable while
the personal link is gone.

> This cannot be undone. There is no recycle bin, and the confirmation dialog is
> the only prompt you get.

### 3.5 System settings

**Reports → System settings**

The **minimum donation interval** is the number of days between donations. The
default is 84 (12 weeks). Change it and press **Update interval**; it applies to
the very next donor at the counter, with no restart and no rebuild. Reports use
the same value, so they cannot drift from the counter.

Accepted range is 1–365 days.

> **This is a clinical decision, not an administrative one.** Lowering the
> interval permits donors to give blood sooner than the standard guidance.
> Do not change it without medical authority.

---

## Part 4 — The donor

Donors never sign in — they have no account. This section is what to tell them.

**What to bring to every donation**

- The blood donation card
- A photo identity document
- The four-digit PIN, if one was chosen at registration

**Why they are asked for identity every time.** The card proves which record to
open; the photograph and ID prove the card belongs to the person holding it.
Without that check a card could be borrowed or shared, and someone's donation
history — the thing that keeps them safe from donating too soon — would be wrong.

**What is on the card.** Name, blood group, photograph, card number and a QR
code. **The national identity number is not on the card**, and the QR contains
only the card number, so a lost card gives a finder nothing useful.

**If the card is lost.** Report it at any centre. It is deactivated on the spot
and a replacement is printed. Donation history is attached to the donor, so
nothing is lost.

**If they are turned away.** Not enough days have passed since their last
donation. Staff will tell them the earliest date they can return. The interval
exists to protect them, and it is not negotiable at the counter.

**Their privacy.** Staff see the identity number only as `S••••567A` — first
character and last four. The full number is stored but never shown on screen.

---

## Reference

### Practice data

The demonstration database is arranged so every outcome can be shown. All PINs
are `1234`.

| Donor | Card number | Try it to see |
|---|---|---|
| Min Khant Zaw | `BDC-2026-6MPT5RG9` | A first-time donor — **eligible** |
| Hein Thu | `BDC-2026-4KDQ7M2P` | 200 days since last — **eligible** |
| Kyaw Zin Latt | `BDC-2026-7QJ2FE5S` | Exactly 84 days — **eligible**, the boundary |
| Thida Win | `BDC-2026-3VZ8KA1N` | 83 days — **not eligible** by one day |
| Su Su Aung | `BDC-2026-9XR3B8VT` | 30 days — **not eligible**, 54 to go |
| *(Kyaw Zin Latt's old card)* | `BDC-2026-1HN6CY4W` | A **LOST** card being refused |
| *(nothing)* | `BDC-2026-0000XXXX` | **Card not found** |

Registering national ID `S1234567A` again demonstrates duplicate rejection.

### Card numbers

Format `BDC-2026-XXXXXXXX`. The letters **I, L, O and U are never used**, so they
cannot be confused with 1 and 0. If you type one by mistake the system corrects
it for you — `O` becomes `0`, `I` and `L` become `1`.

Card numbers are not sequential. Holding one card tells you nothing about any other.

### Keyboard and scanner

- A handheld QR/barcode scanner behaves as a fast keyboard and presses Enter, so
  it submits the lookup with no extra step.
- The scan field takes focus automatically on the dashboard and the verify screen.
- PIN fields accept digits only and stop at four.

### Getting help

| Problem | Fix |
|---|---|
| Cannot reach the site | The application is not running — see [SETUP.md](SETUP.md) |
| *Communications link failure* | The database is not running — see [SETUP.md](SETUP.md) |
| Signed out unexpectedly | 20-minute idle timeout; sign in again |
| **403** | Administrator-only page; ask an administrator |
| **404** | The donor or card does not exist, or an out-of-date link |
| **500** | Nothing was saved — donation recording is all-or-nothing. Tell an administrator what you were doing |
| A photograph shows a grey placeholder | No photo was uploaded for that donor. Add one from **Edit details** |
| The camera will not open | Permission was refused, or the site is not on HTTPS/localhost. Type the card number |
| A donor cannot be deleted | They have donations. That is intended — history is append-only |

### Glossary

| Term | Meaning |
|---|---|
| **Active card** | The one card a donor may currently present. At most one per donor |
| **Eligible** | Enough days have passed since the last donation |
| **Masked ID** | `S••••567A` — first character and last four, the rest hidden |
| **Verification** | Confirming the person presenting the card is its registered owner |
| **Audit log** | The permanent record of every verification attempt, successful or not |
| **Detached log entry** | An audit row whose donor was deleted. The event is kept; the link to the person is not |
| **Interval** | Minimum days between donations. Configurable; 84 by default |

---

For installation and running the system, see **[SETUP.md](SETUP.md)**.
For the code and how it fits together, see **[ARCHITECTURE.md](ARCHITECTURE.md)**
and **[TEAM-GUIDE.md](TEAM-GUIDE.md)**.
