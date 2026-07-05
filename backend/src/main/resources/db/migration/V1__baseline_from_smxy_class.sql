-- V1 · Baseline schema extracted from legacy smxy_class.sql.
-- Removed: subjectall table, `trigger` (AFTER INSERT on subject), `intoALL` procedure.
-- Field names are kept close to the legacy schema; renaming happens in V2.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- user ----------
CREATE TABLE `user` (
  `id`          INT UNSIGNED     NOT NULL AUTO_INCREMENT,
  `realName`    VARCHAR(20)      NULL,
  `password`    VARCHAR(64)      NULL,
  `priviledge`  INT              NULL COMMENT '0=STUDENT, 1=TEACHER (legacy)',
  `loginName`   VARCHAR(50)      NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- student ----------
CREATE TABLE `student` (
  `id`       INT UNSIGNED   NOT NULL,
  `tel`      VARCHAR(11)    NULL,
  `address`  VARCHAR(50)    NULL,
  `grade`    INT            NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_student_user`
    FOREIGN KEY (`id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- teacher ----------
CREATE TABLE `teacher` (
  `id`   INT UNSIGNED  NOT NULL,
  `tel`  VARCHAR(11)   NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_teacher_user`
    FOREIGN KEY (`id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- subject ----------
CREATE TABLE `subject` (
  `id`       INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  `name`     VARCHAR(50)   NULL,
  `teacher`  INT UNSIGNED  NULL,
  `grade`    INT           NULL,
  PRIMARY KEY (`id`),
  KEY `idx_subject_teacher` (`teacher`),
  CONSTRAINT `fk_subject_teacher`
    FOREIGN KEY (`teacher`) REFERENCES `teacher` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- score ----------
CREATE TABLE `score` (
  `id`       INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  `subject`  INT UNSIGNED  NULL,
  `teacher`  INT UNSIGNED  NULL,
  `stu`      INT UNSIGNED  NULL,
  `score`    INT           NULL,
  `grade`    INT           NULL,
  PRIMARY KEY (`id`),
  KEY `idx_score_stu`     (`stu`),
  KEY `idx_score_teacher` (`teacher`),
  KEY `idx_score_subject` (`subject`),
  CONSTRAINT `fk_score_student`
    FOREIGN KEY (`stu`) REFERENCES `student` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_score_teacher`
    FOREIGN KEY (`teacher`) REFERENCES `teacher` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_score_subject`
    FOREIGN KEY (`subject`) REFERENCES `subject` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
