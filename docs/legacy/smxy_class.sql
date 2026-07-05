/*
Navicat MySQL Data Transfer

Source Server         : localhost
Source Server Version : 50022
Source Host           : localhost:3308
Source Database       : smxy_class

Target Server Type    : MYSQL
Target Server Version : 50022
File Encoding         : 65001

Date: 2016-12-30 08:58:58
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for score
-- ----------------------------
DROP TABLE IF EXISTS `score`;
CREATE TABLE `score` (
  `id` int(11) NOT NULL auto_increment,
  `subject` int(11) default NULL,
  `teacher` int(11) default NULL,
  `stu` int(11) default NULL,
  `score` int(11) default NULL,
  `grade` int(11) default NULL,
  PRIMARY KEY  (`id`),
  KEY `foreignKey_StuSco` (`stu`),
  KEY `score_ibfk_1` (`teacher`),
  CONSTRAINT `foreignKey_StuSco` FOREIGN KEY (`stu`) REFERENCES `student` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `score_ibfk_1` FOREIGN KEY (`teacher`) REFERENCES `teacher` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of score
-- ----------------------------
INSERT INTO `score` VALUES ('1', '4', '2', '6', '100', '1');
INSERT INTO `score` VALUES ('2', '5', '2', '3', '99', '3');
INSERT INTO `score` VALUES ('3', '6', '2', '3', '100', '3');
INSERT INTO `score` VALUES ('4', '8', '1', '6', '59', '1');

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student` (
  `id` int(11) NOT NULL,
  `tel` varchar(11) default NULL,
  `address` varchar(50) default NULL,
  `grade` int(11) default NULL,
  PRIMARY KEY  (`id`),
  CONSTRAINT `foreignKey_stuId` FOREIGN KEY (`id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES ('3', '18075853353', 'SMU', '3');
INSERT INTO `student` VALUES ('4', '12345678901', 'SMU', '2');
INSERT INTO `student` VALUES ('5', '12345678901', 'SMU', '3');
INSERT INTO `student` VALUES ('6', '12345678901', '三大', '1');

-- ----------------------------
-- Table structure for subject
-- ----------------------------
DROP TABLE IF EXISTS `subject`;
CREATE TABLE `subject` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(20) default NULL,
  `teacher` int(11) default NULL,
  `grade` int(11) default NULL,
  PRIMARY KEY  (`id`),
  KEY `foreignKey_Teacher` (`teacher`),
  CONSTRAINT `foreignKey_Teacher` FOREIGN KEY (`teacher`) REFERENCES `teacher` (`id`) ON DELETE SET NULL ON UPDATE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of subject
-- ----------------------------
INSERT INTO `subject` VALUES ('1', '计算机导论', '1', '1');
INSERT INTO `subject` VALUES ('2', '汇编语言程序设计', '1', '2');
INSERT INTO `subject` VALUES ('3', '数据库原理与应用', '1', '2');
INSERT INTO `subject` VALUES ('4', '计算机软硬件维护', '2', '1');
INSERT INTO `subject` VALUES ('5', 'Java EE', '2', '3');
INSERT INTO `subject` VALUES ('6', '云计算', '2', '3');
INSERT INTO `subject` VALUES ('7', '计算机导论', '2', '1');

-- ----------------------------
-- Table structure for subjectall
-- ----------------------------
DROP TABLE IF EXISTS `subjectall`;
CREATE TABLE `subjectall` (
  `id` int(11) default NULL,
  `name` varchar(255) default NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of subjectall
-- ----------------------------
INSERT INTO `subjectall` VALUES ('1', '计算机导论');
INSERT INTO `subjectall` VALUES ('2', '汇编语言程序设计');
INSERT INTO `subjectall` VALUES ('3', '数据库原理与应用');
INSERT INTO `subjectall` VALUES ('4', '计算机软硬件维护');
INSERT INTO `subjectall` VALUES ('5', 'Java EE');
INSERT INTO `subjectall` VALUES ('6', '云计算');
INSERT INTO `subjectall` VALUES ('7', '计算机导论');
INSERT INTO `subjectall` VALUES ('8', 'C语言程序设计');

-- ----------------------------
-- Table structure for teacher
-- ----------------------------
DROP TABLE IF EXISTS `teacher`;
CREATE TABLE `teacher` (
  `id` int(11) NOT NULL,
  `tel` varchar(11) default NULL,
  PRIMARY KEY  (`id`),
  CONSTRAINT `foreignKey_Teacherid` FOREIGN KEY (`id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of teacher
-- ----------------------------
INSERT INTO `teacher` VALUES ('1', '18065853353');
INSERT INTO `teacher` VALUES ('2', '18065853353');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int(11) NOT NULL auto_increment,
  `realName` varchar(20) default NULL,
  `password` varchar(16) default NULL,
  `priviledge` int(11) default NULL,
  `loginName` varchar(50) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES ('1', '田', '123456', '1', 'ttt');
INSERT INTO `user` VALUES ('2', '伍', '123456', '1', 'www');
INSERT INTO `user` VALUES ('3', '黄XX', '123456', '0', 'hhh');
INSERT INTO `user` VALUES ('4', '张三', '123456', '0', 'zs');
INSERT INTO `user` VALUES ('5', '李四', '123456', '0', 'ls');
INSERT INTO `user` VALUES ('6', '王五', '123456', '0', 'ww');

-- ----------------------------
-- Procedure structure for intoALL
-- ----------------------------
DROP PROCEDURE IF EXISTS `intoALL`;
DELIMITER ;;
CREATE DEFINER=`root`@`LocalHost` PROCEDURE `intoALL`(`cid`  int,`cname` varchar(20))
BEGIN
	#Routine body goes here...
  #Inorder to repair BUGs...ORZ...ORZ...
  INSERT INTO subjectall(id,name) VALUES (cid,cname);
END
;;
DELIMITER ;
DROP TRIGGER IF EXISTS `trigger`;
DELIMITER ;;
CREATE TRIGGER `trigger` AFTER INSERT ON `subject` FOR EACH ROW begin

declare cid int;
declare cname varchar(20);

set cid = (New.id);
set cname = (New.name);

call intoAll(cid,cname);

end
;;
DELIMITER ;
