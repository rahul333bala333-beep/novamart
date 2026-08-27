-- Demo accounts.
--
-- These are development fixtures, not production data. The passwords they hash
-- are published in the README so the project can be demonstrated, which is
-- exactly why this migration must never run against a real deployment. Guard it
-- by pointing `spring.flyway.locations` away from `db/seed` in production, or by
-- deleting these rows during the first real migration.
--
-- The digests below are genuine BCrypt hashes at strength 10, generated with
-- Spring Security's BCryptPasswordEncoder and round-trip verified. They are not
-- placeholders: signing in with the documented passwords actually works.
--
-- User ids are fixed rather than random so that seed data in other services
-- (orders belonging to a shopper, notifications addressed to one) can reference
-- them without a lookup. Each service still owns its own database; a UUID
-- travelling between them is a reference, not a shared table.
--
--   admin@novamart.dev    Admin@12345    ADMIN
--   demo@novamart.dev     Demo@12345     USER
--   rohan@example.com     Shopper@123    USER
--   meera@example.com     Shopper@123    USER

INSERT INTO users (id, first_name, last_name, email, password_hash, phone, enabled, created_at, updated_at) VALUES
  ('11111111-1111-4111-8111-111111111111', 'Priya',  'Raghavan', 'admin@novamart.dev', '$2a$10$aw4kXeijzIB0KCP9pY9DtOcbB4Hm4l8jcgPCH7hDCVh0ujEKL80F2', '+91 98450 11001', TRUE, TIMESTAMP '2026-01-05 09:00:00', TIMESTAMP '2026-01-05 09:00:00'),
  ('22222222-2222-4222-8222-222222222222', 'Ananya', 'Iyer',     'demo@novamart.dev',  '$2a$10$s3vatcbNv79hESK03Qb99OX0b45iF/JwtxaJZBpR8m299pXtnXXIa',  '+91 98450 22002', TRUE, TIMESTAMP '2026-01-08 11:20:00', TIMESTAMP '2026-01-08 11:20:00'),
  ('33333333-3333-4333-8333-333333333333', 'Rohan',  'Mehta',    'rohan@example.com',  '$2a$10$rXdWINt2gBSQqrBX9Hfc6OMWLFfyE/ljHeL3GapRilDjofEKxue/G',  '+91 98450 33003', TRUE, TIMESTAMP '2026-01-14 16:45:00', TIMESTAMP '2026-01-14 16:45:00'),
  ('44444444-4444-4444-8444-444444444444', 'Meera',  'Krishnan', 'meera@example.com',  '$2a$10$rXdWINt2gBSQqrBX9Hfc6OMWLFfyE/ljHeL3GapRilDjofEKxue/G',  '+91 98450 44004', TRUE, TIMESTAMP '2026-01-21 08:05:00', TIMESTAMP '2026-01-21 08:05:00');

-- The administrator holds ADMIN only. Granting ADMIN in addition to USER would
-- make "is this person a shopper?" ambiguous everywhere it is asked.
INSERT INTO user_roles (user_id, role) VALUES
  ('11111111-1111-4111-8111-111111111111', 'ADMIN'),
  ('22222222-2222-4222-8222-222222222222', 'USER'),
  ('33333333-3333-4333-8333-333333333333', 'USER'),
  ('44444444-4444-4444-8444-444444444444', 'USER');

INSERT INTO addresses (id, user_id, label, recipient_name, phone, line1, line2, city, state, postal_code, country, is_default, created_at) VALUES
  ('a1111111-1111-4111-8111-111111111111', '22222222-2222-4222-8222-222222222222', 'Home',   'Ananya Iyer',    '+91 98450 22002', '14 Brigade Gardens', '2nd Cross, Koramangala',  'Bengaluru', 'Karnataka',   '560034', 'India', TRUE,  TIMESTAMP '2026-01-08 11:30:00'),
  ('a2222222-2222-4222-8222-222222222222', '22222222-2222-4222-8222-222222222222', 'Office', 'Ananya Iyer',    '+91 98450 22002', 'Tower B, Embassy Tech Park', 'Outer Ring Road',    'Bengaluru', 'Karnataka',   '560103', 'India', FALSE, TIMESTAMP '2026-01-09 09:15:00'),
  ('a3333333-3333-4333-8333-333333333333', '33333333-3333-4333-8333-333333333333', 'Home',   'Rohan Mehta',    '+91 98450 33003', '27 Carter Road',      'Bandra West',            'Mumbai',    'Maharashtra', '400050', 'India', TRUE,  TIMESTAMP '2026-01-14 17:00:00'),
  ('a4444444-4444-4444-8444-444444444444', '44444444-4444-4444-8444-444444444444', 'Home',   'Meera Krishnan', '+91 98450 44004', '8 Alwarpet Street',   NULL,                     'Chennai',   'Tamil Nadu',  '600018', 'India', TRUE,  TIMESTAMP '2026-01-21 08:20:00');
