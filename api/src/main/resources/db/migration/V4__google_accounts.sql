-- V4__google_accounts.sql
-- Accounts that sign in with Google have no password, and a replaced account is kept disabled.

ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users ADD COLUMN google_subject VARCHAR(255);
ALTER TABLE users ADD COLUMN disabled_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE users ADD CONSTRAINT uk_users_google_subject UNIQUE (google_subject);
