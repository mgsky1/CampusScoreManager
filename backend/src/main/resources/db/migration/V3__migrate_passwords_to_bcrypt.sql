-- V3 · Replace legacy plaintext `password` column with `password_hash` (BCrypt).
--
-- Because BCrypt requires per-row random salts, we cannot recompute per-user
-- hashes purely in SQL. Legacy passwords are all "123456" (see smxy_class.sql).
-- We seed all rows with a single pre-computed BCrypt hash of "123456"
-- (strength=10). Users MUST rotate their password after first login
-- (documented in docs/MIGRATION.md). New accounts are always hashed via
-- BCryptPasswordEncoder in the application layer.

ALTER TABLE `user`
  ADD COLUMN `password_hash` VARCHAR(72) NOT NULL DEFAULT '' AFTER `password`;

-- BCrypt hash of "123456" @ strength=10.
-- Generated once, deterministic, safe to commit (users must rotate).
UPDATE `user`
   SET `password_hash` = '$2a$10$CwTycUXWue0Thq9StjUM0uG8BnUfbEgXpwGSWmzmOTa7NZl7gLMK2';

ALTER TABLE `user`
  MODIFY COLUMN `password_hash` VARCHAR(72) NOT NULL,
  DROP   COLUMN `password`;
