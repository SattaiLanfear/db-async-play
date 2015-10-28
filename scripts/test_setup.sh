#!/usr/bin/env sh

echo Setting up PostgreSQL test environment...

psql -U postgres << DONESQL
	CREATE DATABASE play_driver_test_alpha;
	CREATE TABLE play_driver_test_alpha.magic_numbers (
		num INTEGER NOT NULL PRIMARY KEY
	);
	CREATE DATABASE play_driver_test_beta;
	CREATE TABLE play_driver_test_beta.magic_colors (
		color varchar PRIMARY KEY
	);
	CREATE USER alphauser WITH PASSWORD 'testpass';
	GRANT ALL PRIVILAGES ON DATABASE play_driver_test_alpha TO alphauser;
	CREATE USER betauser WITH PASSWORD 'testpass';
	GRANT ALL PRIVILAGES ON DATABASE play_driver_test_beta TO betauser;
DONESQL

echo PostgreSQL setup done..
