
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
DROP TABLE IF EXISTS `account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account` (
  `account_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `full_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgex1lmaqpg0ir5g1f5eftyaa1` (`username`),
  CONSTRAINT `account_chk_1` CHECK ((`account_type` in (_utf8mb4'ADMIN',_utf8mb4'LECTURER',_utf8mb4'STUDENT')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `account_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_role` (
  `account_id` bigint NOT NULL,
  `role` enum('ADMIN','DEAN','LECTURER','STUDENT') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  UNIQUE KEY `UKcedvgb58oehnp1wb4u2g9w6s` (`account_id`,`role`),
  CONSTRAINT `FK1f8y4iy71kb1arff79s71j0dh` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin` (
  `account_id` bigint NOT NULL,
  PRIMARY KEY (`account_id`),
  CONSTRAINT `FKn2eatyxq78i3wg18tt0jf56lw` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `advisor_invitation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `advisor_invitation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `response_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ACCEPTED','CANCELLED','PENDING','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `lecturer_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKknntvhn4dl0rx489l166leqq3` (`lecturer_id`),
  CONSTRAINT `FKknntvhn4dl0rx489l166leqq3` FOREIGN KEY (`lecturer_id`) REFERENCES `lecturer` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `council`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `council` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `topic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5cf9jsnawkp14t09j2oh541bb` (`topic_id`),
  CONSTRAINT `FKep5qsypvy2ueayhvk5opsad1w` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `council_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `council_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` enum('CHAIR','MEMBER','SECRETARY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `score` double DEFAULT NULL,
  `scored_at` datetime(6) DEFAULT NULL,
  `council_id` bigint NOT NULL,
  `lecturer_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKlvoe9jlsms0xn5cdght0anrqq` (`council_id`),
  KEY `FKbi1gb7q0jrwrsbrhetg7yql2a` (`lecturer_id`),
  CONSTRAINT `FKbi1gb7q0jrwrsbrhetg7yql2a` FOREIGN KEY (`lecturer_id`) REFERENCES `lecturer` (`account_id`),
  CONSTRAINT `FKlvoe9jlsms0xn5cdght0anrqq` FOREIGN KEY (`council_id`) REFERENCES `council` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `lecturer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lecturer` (
  `department` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lecturer_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` bigint NOT NULL,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `UK7nm1pia5efgpppxfb3qxkqfqw` (`lecturer_code`),
  CONSTRAINT `FK2gt31e48btt9xql0dof7xlagb` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `link` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_read` bit(1) NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` enum('APPROVAL','INVITATION','REJECTION','REMINDER','RESULT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `recipient_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKowpto3m6fronsho1dmaeufm3q` (`recipient_id`),
  CONSTRAINT `FKowpto3m6fronsho1dmaeufm3q` FOREIGN KEY (`recipient_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `registration_period`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `registration_period` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `council_report_date` date DEFAULT NULL,
  `name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `registration_locked` bit(1) NOT NULL,
  `report_deadline` date DEFAULT NULL,
  `reviewer_score_deadline` date DEFAULT NULL,
  `student_register_end` datetime(6) NOT NULL,
  `student_register_start` datetime(6) NOT NULL,
  `teacher_register_end` datetime(6) NOT NULL,
  `teacher_register_start` datetime(6) NOT NULL,
  `type` enum('COURSE','GRADUATION_THESIS','RESEARCH','SPECIALIZED_THESIS') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `original_file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `size` bigint NOT NULL,
  `stored_file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `submitted_at` datetime(6) NOT NULL,
  `version` int NOT NULL,
  `team_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKbj2vtdllqsxlk3g2vf0yl7vpf` (`team_id`),
  CONSTRAINT `FKg68qlvnrh1bb5qwdhb1u6asg1` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student` (
  `student_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` bigint NOT NULL,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `UKpilb3uo1cimnf1sp86nqcrjsv` (`student_code`),
  CONSTRAINT `FKoootcgotavmpat2yv9o52wx1q` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `registration_rejection_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `leader_id` bigint NOT NULL,
  `period_id` bigint NOT NULL,
  `topic_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9ot35qt0cvqbv9pvxn9k9jngq` (`leader_id`),
  KEY `FKfjw3e255asoyibhin06f0jjif` (`period_id`),
  KEY `FKnj76akoopfln51y0fn69daaji` (`topic_id`),
  CONSTRAINT `FK9ot35qt0cvqbv9pvxn9k9jngq` FOREIGN KEY (`leader_id`) REFERENCES `student` (`account_id`),
  CONSTRAINT `FKfjw3e255asoyibhin06f0jjif` FOREIGN KEY (`period_id`) REFERENCES `registration_period` (`id`),
  CONSTRAINT `FKnj76akoopfln51y0fn69daaji` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_invitation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_invitation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `response_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ACCEPTED','CANCELLED','PENDING','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `student_id` bigint NOT NULL,
  `team_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK3iiu99vn6ty1gj8jhe3xy9a3m` (`student_id`),
  KEY `FKt1324odwq1822y9yy49qagjg9` (`team_id`),
  CONSTRAINT `FK3iiu99vn6ty1gj8jhe3xy9a3m` FOREIGN KEY (`student_id`) REFERENCES `student` (`account_id`),
  CONSTRAINT `FKt1324odwq1822y9yy49qagjg9` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_member` (
  `team_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  KEY `FKfcuimvs9r54exe7dq0b0d0rdx` (`student_id`),
  KEY `FK9ubp79ei4tv4crd0r9n7u5i6e` (`team_id`),
  CONSTRAINT `FK9ubp79ei4tv4crd0r9n7u5i6e` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`),
  CONSTRAINT `FKfcuimvs9r54exe7dq0b0d0rdx` FOREIGN KEY (`student_id`) REFERENCES `student` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `availability` enum('AVAILABLE','PENDING','REGISTERED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `department` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rejection_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('APPROVED','PENDING','PUBLISHED','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `invitation_id` bigint DEFAULT NULL,
  `period_id` bigint NOT NULL,
  `primary_advisor_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmty87ba8w0a6as235gbq0htye` (`invitation_id`),
  KEY `FKmon5yn9uds3v9y9ver4n90f2y` (`period_id`),
  KEY `FKhcgce9p044mmira9p5s2qracj` (`primary_advisor_id`),
  CONSTRAINT `FKfa2mb3s643hip1tyhnbc8vc8l` FOREIGN KEY (`invitation_id`) REFERENCES `advisor_invitation` (`id`),
  CONSTRAINT `FKhcgce9p044mmira9p5s2qracj` FOREIGN KEY (`primary_advisor_id`) REFERENCES `lecturer` (`account_id`),
  CONSTRAINT `FKmon5yn9uds3v9y9ver4n90f2y` FOREIGN KEY (`period_id`) REFERENCES `registration_period` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `topic_advisor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_advisor` (
  `topic_id` bigint NOT NULL,
  `lecturer_id` bigint NOT NULL,
  KEY `FKkwbb4jwiud775rfnr9xwl6dr0` (`lecturer_id`),
  KEY `FK7qife2aigvoc57xhal7houdqx` (`topic_id`),
  CONSTRAINT `FK7qife2aigvoc57xhal7houdqx` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`),
  CONSTRAINT `FKkwbb4jwiud775rfnr9xwl6dr0` FOREIGN KEY (`lecturer_id`) REFERENCES `lecturer` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `topic_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `average_score` double NOT NULL,
  `published` bit(1) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `council_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6h0670gdb27026xj6etefgwg9` (`council_id`),
  CONSTRAINT `FK8raeqft3f4fb73s1rsixqd8g5` FOREIGN KEY (`council_id`) REFERENCES `council` (`id`)
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

