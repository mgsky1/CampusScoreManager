-- V5 · Development-only seed data.
-- Loaded ONLY under the dev profile via spring.flyway.locations.
-- Password for every seeded user is "123456" (BCrypt strength=10).

-- ---------- teachers ----------
INSERT INTO `user` (`id`, `real_name`, `login_name`, `password_hash`, `role`) VALUES
  (1, '田老师', 'ttt', '$2a$10$CwTycUXWue0Thq9StjUM0uG8BnUfbEgXpwGSWmzmOTa7NZl7gLMK2', 'TEACHER'),
  (2, '伍老师', 'www', '$2a$10$CwTycUXWue0Thq9StjUM0uG8BnUfbEgXpwGSWmzmOTa7NZl7gLMK2', 'TEACHER');
INSERT INTO `teacher` (`id`, `tel`) VALUES
  (1, '18065853353'),
  (2, '18065853354');

-- ---------- students ----------
INSERT INTO `user` (`id`, `real_name`, `login_name`, `password_hash`, `role`) VALUES
  (3, '黄同学', 'hhh', '$2a$10$CwTycUXWue0Thq9StjUM0uG8BnUfbEgXpwGSWmzmOTa7NZl7gLMK2', 'STUDENT'),
  (4, '张三',   'zs',  '$2a$10$CwTycUXWue0Thq9StjUM0uG8BnUfbEgXpwGSWmzmOTa7NZl7gLMK2', 'STUDENT'),
  (5, '李四',   'ls',  '$2a$10$CwTycUXWue0Thq9StjUM0uG8BnUfbEgXpwGSWmzmOTa7NZl7gLMK2', 'STUDENT'),
  (6, '王五',   'ww',  '$2a$10$CwTycUXWue0Thq9StjUM0uG8BnUfbEgXpwGSWmzmOTa7NZl7gLMK2', 'STUDENT');
INSERT INTO `student` (`id`, `tel`, `address`, `grade`) VALUES
  (3, '18075853353', 'SMU', 3),
  (4, '12345678901', 'SMU', 2),
  (5, '12345678901', 'SMU', 3),
  (6, '12345678901', '三大', 1);

-- ---------- subjects (teacher授课) ----------
INSERT INTO `subject` (`id`, `name`, `teacher_id`, `grade`) VALUES
  (1, '计算机导论',       1, 1),
  (2, '汇编语言程序设计', 1, 2),
  (3, '数据库原理与应用', 1, 2),
  (4, '计算机软硬件维护', 2, 1),
  (5, 'Java EE',           2, 3),
  (6, '云计算',           2, 3);

-- ---------- scores ----------
INSERT INTO `score` (`student_id`, `subject_id`, `teacher_id`, `score`, `grade`) VALUES
  (6, 4, 2, 100, 1),
  (3, 5, 2,  99, 3),
  (3, 6, 2, 100, 3),
  (5, 2, 1,  88, 2),
  (4, 1, 1,  75, 1),
  (5, 3, 1,  92, 2);
