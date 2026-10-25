
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `account_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_roles` (
  `account_id` bigint NOT NULL,
  `role` enum('ADMIN','DEAN','LECTURER','STUDENT') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  UNIQUE KEY `UK9fn2s5xwoa52nvgx3gh7s0lnu` (`account_id`,`role`),
  CONSTRAINT `FK61h48dsir3h82pxbq3cwgp0ce` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `accounts` (
  `account_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_enabled` bit(1) NOT NULL,
  `full_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKk8h1bgqoplx0rkngj01pm1rgp` (`username`),
  CONSTRAINT `accounts_chk_1` CHECK ((`account_type` in (_utf8mb4'ADMIN',_utf8mb4'LECTURER',_utf8mb4'STUDENT')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `admins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admins` (
  `account_id` bigint NOT NULL,
  PRIMARY KEY (`account_id`),
  CONSTRAINT `FKji7l5mi8tqv73veb5cd90hupx` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `advisor_invitations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `advisor_invitations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `response_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ACCEPTED','CANCELLED','PENDING','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `lecturer_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9a3ne7aa8k153rjv9tml4slmx` (`lecturer_id`),
  CONSTRAINT `FK9a3ne7aa8k153rjv9tml4slmx` FOREIGN KEY (`lecturer_id`) REFERENCES `lecturers` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `announcements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `council_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `council_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` enum('CHAIR','MEMBER','SECRETARY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `score` double DEFAULT NULL,
  `scored_at` datetime(6) DEFAULT NULL,
  `council_id` bigint NOT NULL,
  `lecturer_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgiyq6tprvjaf8gi2trlretwsr` (`council_id`),
  KEY `FKa06vd06j4qwfqc0v6264u580i` (`lecturer_id`),
  CONSTRAINT `FKa06vd06j4qwfqc0v6264u580i` FOREIGN KEY (`lecturer_id`) REFERENCES `lecturers` (`account_id`),
  CONSTRAINT `FKgiyq6tprvjaf8gi2trlretwsr` FOREIGN KEY (`council_id`) REFERENCES `councils` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `councils`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `councils` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `topic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKb57m4y3ynmy39jcig6yv0no21` (`topic_id`),
  CONSTRAINT `FKexe674bw39t3p9hy2b4q0lstn` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `departments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `departments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKl7tivi5261wxdnvo6cct9gg6t` (`code`),
  UNIQUE KEY `UKj6cwks7xecs5jov19ro8ge3qk` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `lecturers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lecturers` (
  `lecturer_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` bigint NOT NULL,
  `department_id` bigint DEFAULT NULL,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `UKk6vsb5jy1uq700gx4a6brvtpv` (`lecturer_code`),
  KEY `FKpowy6g9rglsdytsdu6c9fymox` (`department_id`),
  CONSTRAINT `FKpowy6g9rglsdytsdu6c9fymox` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
  CONSTRAINT `FKrr0exae0eimybpvxqyhb93ur9` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `link` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_read` bit(1) NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` enum('APPROVAL','INVITATION','REJECTION','REMINDER','RESULT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `recipient_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKrphdmgvl924ajapmo7l8hyx3i` (`recipient_id`),
  CONSTRAINT `FKrphdmgvl924ajapmo7l8hyx3i` FOREIGN KEY (`recipient_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `registration_periods`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `registration_periods` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `council_report_date` date DEFAULT NULL,
  `name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_registration_locked` bit(1) NOT NULL,
  `report_deadline` date DEFAULT NULL,
  `reviewer_score_deadline` date DEFAULT NULL,
  `student_register_end` datetime(6) NOT NULL,
  `student_register_start` datetime(6) NOT NULL,
  `lecturer_register_end` datetime(6) NOT NULL,
  `lecturer_register_start` datetime(6) NOT NULL,
  `type` enum('COURSE_PROJECT','GRADUATION_THESIS','RESEARCH','SPECIALIZED_PROJECT') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `original_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` bigint NOT NULL,
  `stored_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `submitted_at` datetime(6) NOT NULL,
  `version` int NOT NULL,
  `team_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKopxteynf40iipggm1pj2fbwaq` (`team_id`),
  CONSTRAINT `FKfwjm5nsj0jaguhk0lnayr5ttt` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `student_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` bigint NOT NULL,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `UKcgcf3r5xk73o0etbduc1qxnol` (`student_code`),
  CONSTRAINT `FKsd9hvvjnstr8is6vhbv4dun1m` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_invitations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_invitations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `response_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ACCEPTED','CANCELLED','PENDING','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `student_id` bigint NOT NULL,
  `team_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKc1pcjkkxnp0chthl3mlhcm4tc` (`student_id`),
  KEY `FKn2spna496q3cr3fmqc8gda2my` (`team_id`),
  CONSTRAINT `FKc1pcjkkxnp0chthl3mlhcm4tc` FOREIGN KEY (`student_id`) REFERENCES `students` (`account_id`),
  CONSTRAINT `FKn2spna496q3cr3fmqc8gda2my` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_members` (
  `team_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  KEY `FKj9k6kbn0j42ffi6ytxujo6uwe` (`student_id`),
  KEY `FKtgca08el3ofisywcf11f0f76t` (`team_id`),
  CONSTRAINT `FKj9k6kbn0j42ffi6ytxujo6uwe` FOREIGN KEY (`student_id`) REFERENCES `students` (`account_id`),
  CONSTRAINT `FKtgca08el3ofisywcf11f0f76t` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `teams`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teams` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `registration_rejection_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `leader_id` bigint NOT NULL,
  `period_id` bigint NOT NULL,
  `topic_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teams_topic` (`topic_id`),
  UNIQUE KEY `uk_teams_period_leader` (`period_id`, `leader_id`),
  KEY `FK9ii40cq2mxptdb0pkl6xltf6m` (`leader_id`),
  KEY `FKeks2m6tbwkjm4ndag1yl3n40m` (`period_id`),
  KEY `FKh8e1pbbvk3fqkhlyxk4i7pgxi` (`topic_id`),
  CONSTRAINT `FK9ii40cq2mxptdb0pkl6xltf6m` FOREIGN KEY (`leader_id`) REFERENCES `students` (`account_id`),
  CONSTRAINT `FKeks2m6tbwkjm4ndag1yl3n40m` FOREIGN KEY (`period_id`) REFERENCES `registration_periods` (`id`),
  CONSTRAINT `FKh8e1pbbvk3fqkhlyxk4i7pgxi` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `topic_advisors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_advisors` (
  `topic_id` bigint NOT NULL,
  `lecturer_id` bigint NOT NULL,
  KEY `FKcrjk3819j7jif3dlpqweiel7j` (`lecturer_id`),
  KEY `FK1u4y32dfyj81l34vpfii5ar9q` (`topic_id`),
  CONSTRAINT `FK1u4y32dfyj81l34vpfii5ar9q` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`),
  CONSTRAINT `FKcrjk3819j7jif3dlpqweiel7j` FOREIGN KEY (`lecturer_id`) REFERENCES `lecturers` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `topic_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_results` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `average_council_score` double NOT NULL,
  `final_score` double DEFAULT NULL,
  `is_published` bit(1) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `council_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKdtyy37urtj1fnihfhe3vxvq5a` (`council_id`),
  CONSTRAINT `FK5bgomwqw0hr67d5o2jdamvc6l` FOREIGN KEY (`council_id`) REFERENCES `councils` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `topics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `registration_status` enum('AVAILABLE','PENDING','REGISTERED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `name` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `rejection_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reviewer_comment` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reviewer_score` double DEFAULT NULL,
  `reviewer_scored_at` datetime(6) DEFAULT NULL,
  `approval_status` enum('APPROVED','PENDING','PUBLISHED','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `department_id` bigint DEFAULT NULL,
  `co_advisor_invitation_id` bigint DEFAULT NULL,
  `period_id` bigint NOT NULL,
  `primary_advisor_id` bigint NOT NULL,
  `reviewer_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKpwfxnlxxx1f4buu68tosr8km3` (`co_advisor_invitation_id`),
  KEY `FKnjufsnsv9rtvmyp7seo42dyxu` (`department_id`),
  KEY `FK4x8y71cgt01hk3myrm6mgs6v0` (`period_id`),
  KEY `FKayb9o1sw7oguvvdwgeluw92mx` (`primary_advisor_id`),
  KEY `FKrnphp8urthmtjr9of59dlgs8e` (`reviewer_id`),
  CONSTRAINT `FK1ue6dsthr43m43urrndylnsdk` FOREIGN KEY (`co_advisor_invitation_id`) REFERENCES `advisor_invitations` (`id`),
  CONSTRAINT `FK4x8y71cgt01hk3myrm6mgs6v0` FOREIGN KEY (`period_id`) REFERENCES `registration_periods` (`id`),
  CONSTRAINT `FKayb9o1sw7oguvvdwgeluw92mx` FOREIGN KEY (`primary_advisor_id`) REFERENCES `lecturers` (`account_id`),
  CONSTRAINT `FKnjufsnsv9rtvmyp7seo42dyxu` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
  CONSTRAINT `FKrnphp8urthmtjr9of59dlgs8e` FOREIGN KEY (`reviewer_id`) REFERENCES `lecturers` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

