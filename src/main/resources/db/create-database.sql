CREATE DATABASE IF NOT EXISTS spexregister CHARACTER SET utf8 COLLATE utf8_swedish_ci;
CREATE USER 'spexregister'@'%' IDENTIFIED BY 'spexregister';
GRANT ALL ON spexregister.* TO 'spexregister'@'%';
