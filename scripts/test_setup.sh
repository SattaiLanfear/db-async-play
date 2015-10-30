#!/usr/bin/env sh

echo Setting up PostgreSQL test environment...

psql -U postgres << DONESQL
	CREATE USER alphauser WITH PASSWORD 'testpass';
	CREATE USER betauser WITH PASSWORD 'testpass';
	CREATE DATABASE play_driver_test_alpha WITH OWNER alphauser;
	\connect play_driver_test_alpha
	CREATE TABLE magic_numbers (
		num INTEGER NOT NULL PRIMARY KEY
	);
	ALTER TABLE magic_numbers OWNER TO alphauser;
	CREATE DATABASE play_driver_test_beta WITH OWNER betauser;
	\connect play_driver_test_beta
	CREATE TABLE magic_colors (
		color varchar NOT NULL PRIMARY KEY
	);
	ALTER TABLE magic_colors OWNER TO betauser;
	GRANT ALL PRIVILEGES ON DATABASE play_driver_test_alpha TO alphauser;
	GRANT ALL PRIVILEGES ON DATABASE play_driver_test_beta TO betauser;
DONESQL

echo PostgreSQL setup done...

echo Setting up MySQL test environment...

mysql -u root << DONESQL
	CREATE DATABASE play_driver_test_alpha;
	CREATE TABLE IF NOT EXISTS play_driver_test_alpha.magic_numbers (
        num INT NOT NULL,
        PRIMARY KEY (num)
    );
	CREATE DATABASE play_driver_test_beta;
	CREATE TABLE IF NOT EXISTS play_driver_test_beta.magic_colors (
		color VARCHAR(128) NOT NULL,
		PRIMARY KEY (color)
	);
	GRANT ALL PRIVILEGES ON play_driver_test_alpha.* TO 'alphauser'@'localhost' IDENTIFIED BY 'testpass' WITH GRANT OPTION;
	GRANT ALL PRIVILEGES ON play_driver_test_beta.* TO 'betauser'@'localhost' IDENTIFIED BY 'testpass' WITH GRANT OPTION;
DONESQL

echo MySQL setup done...


