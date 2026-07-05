-- V2 · Rename legacy columns to snake_case, tighten types, add timestamps.
-- Also converts legacy INT priviledge -> VARCHAR role enum (STUDENT / TEACHER).

SET FOREIGN_KEY_CHECKS = 0;

-- ---------- user ----------
ALTER TABLE `user`
  CHANGE COLUMN `realName`   `real_name`  VARCHAR(20)  NOT NULL,
  CHANGE COLUMN `loginName`  `login_name` VARCHAR(50)  NOT NULL,
  ADD    COLUMN `role`       VARCHAR(16)  NOT NULL DEFAULT 'STUDENT' AFTER `password`,
  ADD    COLUMN `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD    COLUMN `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP;

UPDATE `user` SET `role` = CASE `priviledge`
    WHEN 1 THEN 'TEACHER'
    WHEN 0 THEN 'STUDENT'
    ELSE 'STUDENT'
END;

ALTER TABLE `user`
  DROP COLUMN `priviledge`,
  ADD  CONSTRAINT `ck_user_role` CHECK (`role` IN ('STUDENT', 'TEACHER'));

-- ---------- student ----------
ALTER TABLE `student`
  MODIFY COLUMN `tel`     VARCHAR(11) NOT NULL DEFAULT '',
  MODIFY COLUMN `address` VARCHAR(50) NOT NULL DEFAULT '',
  MODIFY COLUMN `grade`   SMALLINT    NOT NULL DEFAULT 1,
  ADD    CONSTRAINT `ck_student_grade` CHECK (`grade` BETWEEN 1 AND 6);

-- ---------- teacher ----------
ALTER TABLE `teacher`
  MODIFY COLUMN `tel` VARCHAR(11) NULL;

-- ---------- subject ----------
ALTER TABLE `subject`
  DROP FOREIGN KEY `fk_subject_teacher`;
ALTER TABLE `subject`
  DROP KEY `idx_subject_teacher`,
  CHANGE COLUMN `teacher` `teacher_id` INT UNSIGNED NOT NULL,
  MODIFY COLUMN `name`  VARCHAR(50) NOT NULL,
  MODIFY COLUMN `grade` SMALLINT    NOT NULL DEFAULT 1,
  ADD    COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD    CONSTRAINT `ck_subject_grade` CHECK (`grade` BETWEEN 1 AND 6),
  ADD    KEY `idx_subject_teacher` (`teacher_id`),
  ADD    CONSTRAINT `fk_subject_teacher`
         FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`)
         ON DELETE CASCADE ON UPDATE CASCADE;

-- ---------- score ----------
ALTER TABLE `score`
  DROP FOREIGN KEY `fk_score_student`,
  DROP FOREIGN KEY `fk_score_teacher`,
  DROP FOREIGN KEY `fk_score_subject`;
ALTER TABLE `score`
  DROP KEY `idx_score_stu`,
  DROP KEY `idx_score_teacher`,
  DROP KEY `idx_score_subject`,
  CHANGE COLUMN `stu`     `student_id` INT UNSIGNED NOT NULL,
  CHANGE COLUMN `subject` `subject_id` INT UNSIGNED NOT NULL,
  CHANGE COLUMN `teacher` `teacher_id` INT UNSIGNED NOT NULL,
  MODIFY COLUMN `score`   SMALLINT NOT NULL,
  MODIFY COLUMN `grade`   SMALLINT NOT NULL,
  ADD    COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD    COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP,
  ADD    CONSTRAINT `ck_score_range` CHECK (`score` BETWEEN 0 AND 100),
  ADD    KEY `idx_score_student` (`student_id`),
  ADD    KEY `idx_score_teacher` (`teacher_id`),
  ADD    CONSTRAINT `fk_score_student`
         FOREIGN KEY (`student_id`) REFERENCES `student` (`id`)
         ON DELETE CASCADE ON UPDATE CASCADE,
  ADD    CONSTRAINT `fk_score_teacher`
         FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`)
         ON DELETE CASCADE ON UPDATE CASCADE,
  ADD    CONSTRAINT `fk_score_subject`
         FOREIGN KEY (`subject_id`) REFERENCES `subject` (`id`)
         ON DELETE CASCADE ON UPDATE CASCADE;
-- NOTE: fk_score_subject switches to ON DELETE RESTRICT in V4.

SET FOREIGN_KEY_CHECKS = 1;
