CREATE DATABASE IF NOT EXISTS qldt CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS qldt_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'qldt_admin'@'localhost' IDENTIFIED BY 'QldtAdmin@2026';
CREATE USER IF NOT EXISTS 'qldt_app'@'localhost' IDENTIFIED BY 'QldtApp@2026';
CREATE USER IF NOT EXISTS 'qldt_app'@'%' IDENTIFIED BY 'QldtApp@2026';

GRANT ALL PRIVILEGES ON *.* TO 'qldt_admin'@'localhost' WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON qldt.* TO 'qldt_app'@'localhost';
GRANT ALL PRIVILEGES ON qldt_test.* TO 'qldt_app'@'localhost';
GRANT ALL PRIVILEGES ON qldt.* TO 'qldt_app'@'%';
GRANT ALL PRIVILEGES ON qldt_test.* TO 'qldt_app'@'%';

FLUSH PRIVILEGES;

DROP USER IF EXISTS 'root'@'localhost';
