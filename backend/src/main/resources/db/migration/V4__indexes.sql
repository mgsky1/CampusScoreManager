-- V4 · Unique indexes + tighten score→subject FK to RESTRICT.
--
-- Uses functional index on LOWER(login_name), which requires MySQL 8.0.13+.
-- If you must run on MySQL 5.7, replace with a generated column
-- `login_name_lower VARCHAR(50) GENERATED ALWAYS AS (LOWER(login_name)) STORED`
-- and put the UNIQUE KEY on that column instead.

ALTER TABLE `user`
  ADD UNIQUE KEY `uk_user_login_name_lower` ((LOWER(`login_name`)));

ALTER TABLE `subject`
  ADD UNIQUE KEY `uk_subject_teacher_name` (`teacher_id`, `name`);

ALTER TABLE `score`
  ADD UNIQUE KEY `uk_score_student_subject` (`student_id`, `subject_id`);

-- FR-033: preserve historical scores. Deleting a subject that still has
-- scores must be rejected at the DB layer.
ALTER TABLE `score` DROP FOREIGN KEY `fk_score_subject`;
ALTER TABLE `score`
  ADD CONSTRAINT `fk_score_subject`
      FOREIGN KEY (`subject_id`) REFERENCES `subject` (`id`)
      ON DELETE RESTRICT ON UPDATE CASCADE;
