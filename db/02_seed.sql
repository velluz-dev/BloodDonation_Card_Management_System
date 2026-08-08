-- ---------------------------------------------------------------------------
-- Blood Donation Card Management System - seed data
--
-- Run after 01_schema.sql:  mysql -u root -p < db/02_seed.sql
--
-- Staff logins (passwords are BCrypt cost-12 hashes, generated with jBCrypt):
--     admin / Admin@123   ADMIN
--     staff / Staff@123   STAFF
--     nurse / Nurse@123   STAFF
--
-- Demo donor PIN for every seeded donor: 1234
--
-- Donation dates are relative to CURDATE(), so the eligibility demonstrations
-- below stay meaningful no matter when the seed is run.
-- ---------------------------------------------------------------------------

USE bdcms;

-- ---------------------------------------------------------------------------
-- Staff accounts (FR-1)
-- ---------------------------------------------------------------------------
INSERT INTO users (username, password_hash, full_name, role, is_active) VALUES
  ('admin', '$2a$12$gK6zx5buD9/iRUaLose07eKTAxZO4NPeLgZcoUUgVCqJQ7NQpqfyG', 'System Administrator', 'ADMIN', 1),
  ('staff', '$2a$12$eYQrh8hFy6loY02ezDDDBu8GHa7HPikcZi6hk0eNV5X7exHxh1w6S', 'Aye Aye Myint',        'STAFF', 1),
  ('nurse', '$2a$12$1vUdgMJGWSEKLNNrS2g2d.5yF85.ykgK/fCIdhi/XJmxKjLkbMYXK', 'Nurse Khin Mar',       'STAFF', 1);

-- ---------------------------------------------------------------------------
-- Configurable business rules (NFR Maintainability, FR-5)
-- ---------------------------------------------------------------------------
INSERT INTO settings (setting_key, setting_value, description) VALUES
  ('min_donation_interval_days', '84',
   'Minimum days between whole-blood donations (12 weeks). Proposal S10.1.'),
  ('default_donation_volume_ml', '450',
   'Volume pre-filled on the donation form.'),
  ('centre_name', 'Central Blood Bank',
   'Centre name printed on cards and reports.');

-- ---------------------------------------------------------------------------
-- Demo donors (FR-2). Every donor shares the demo PIN 1234.
-- ---------------------------------------------------------------------------
SET @pin := '$2a$12$3BmOV8ueE0MxOXJFIX5B7ewrMLLP6tRA4aV96BWo5IUub7ozM/D4u';

INSERT INTO donors (national_id, full_name, date_of_birth, gender, blood_group, contact_no, address, pin_hash) VALUES
  ('S1234567A', 'Hein Thu',        '1998-03-14', 'MALE',   'O_POS',  '+95 9 111 2233', '12 Pyay Road, Yangon',      @pin),
  ('S2345678B', 'Su Su Aung',      '1995-11-02', 'FEMALE', 'A_POS',  '+95 9 222 3344', '48 Bogyoke Street, Yangon', @pin),
  ('S3456789C', 'Kyaw Zin Latt',   '2001-06-25', 'MALE',   'B_NEG',  '+95 9 333 4455', '7 Insein Road, Yangon',     @pin),
  ('S4567890D', 'Thida Win',       '1990-01-09', 'FEMALE', 'AB_POS', '+95 9 444 5566', '90 Mandalay Street, Bago',  @pin),
  ('S5678901E', 'Min Khant Zaw',   '1993-09-30', 'MALE',   'O_NEG',  '+95 9 555 6677', '3 Kandawgyi Lane, Yangon',  @pin);

-- ---------------------------------------------------------------------------
-- Issued cards (FR-3). Donor 3 demonstrates a lost-and-reissued card:
-- the superseded row is LOST, only the newest row is ACTIVE.
-- ---------------------------------------------------------------------------
INSERT INTO donor_cards (card_no, donor_id, status, issued_by, issued_on) VALUES
  ('BDC-2026-4KDQ7M2P', 1, 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 400 DAY)),
  ('BDC-2026-9XR3B8VT', 2, 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 300 DAY)),
  ('BDC-2026-1HN6CY4W', 3, 'LOST',   1, DATE_SUB(NOW(), INTERVAL 250 DAY)),
  ('BDC-2026-7QJ2FE5S', 3, 'ACTIVE', 2, DATE_SUB(NOW(), INTERVAL  40 DAY)),
  ('BDC-2026-3VZ8KA1N', 4, 'ACTIVE', 2, DATE_SUB(NOW(), INTERVAL 120 DAY)),
  ('BDC-2026-6MPT5RG9', 5, 'ACTIVE', 2, DATE_SUB(NOW(), INTERVAL  10 DAY));

-- ---------------------------------------------------------------------------
-- Donation history (FR-6), arranged to exercise every eligibility branch:
--
--   Donor 1  last donated 200 days ago  -> ELIGIBLE
--   Donor 2  last donated  30 days ago  -> NOT ELIGIBLE (54 days remaining)
--   Donor 3  last donated  84 days ago  -> ELIGIBLE (exact boundary)
--   Donor 4  last donated  83 days ago  -> NOT ELIGIBLE by one day
--   Donor 5  never donated              -> ELIGIBLE (first-time donor)
-- ---------------------------------------------------------------------------
INSERT INTO donations (donor_id, card_id, donation_date, centre, volume_ml, verified_by) VALUES
  (1, 1, DATE_SUB(CURDATE(), INTERVAL 380 DAY), 'Central Blood Bank', 450, 2),
  (1, 1, DATE_SUB(CURDATE(), INTERVAL 290 DAY), 'Central Blood Bank', 450, 2),
  (1, 1, DATE_SUB(CURDATE(), INTERVAL 200 DAY), 'Central Blood Bank', 450, 3),
  (2, 2, DATE_SUB(CURDATE(), INTERVAL 180 DAY), 'Central Blood Bank', 450, 2),
  (2, 2, DATE_SUB(CURDATE(), INTERVAL  30 DAY), 'Mobile Drive - Bago', 450, 3),
  (3, 3, DATE_SUB(CURDATE(), INTERVAL  84 DAY), 'Central Blood Bank', 450, 2),
  (4, 5, DATE_SUB(CURDATE(), INTERVAL  83 DAY), 'Central Blood Bank', 400, 3);

-- ---------------------------------------------------------------------------
-- Audit trail (FR-4). Seeded with one success and two failures so the audit
-- screen is not empty on a fresh install.
-- ---------------------------------------------------------------------------
INSERT INTO verification_log (card_no, donor_id, staff_id, result, detail, attempted_at) VALUES
  ('BDC-2026-9XR3B8VT', 2,    3, 'SUCCESS',        'Photo and masked ID confirmed by staff',
   DATE_SUB(NOW(), INTERVAL 30 DAY)),
  ('BDC-2026-1HN6CY4W', 3,    2, 'CARD_INACTIVE',  'Card reported lost; reissue required',
   DATE_SUB(NOW(), INTERVAL 41 DAY)),
  ('BDC-2026-0000XXXX', NULL, 2, 'CARD_NOT_FOUND', 'No card matches the number entered',
   DATE_SUB(NOW(), INTERVAL  5 DAY));
