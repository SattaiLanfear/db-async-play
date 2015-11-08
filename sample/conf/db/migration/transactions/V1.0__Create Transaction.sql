
CREATE TABLE transactions (
	id bigserial NOT NULL PRIMARY KEY,
	"to" bigint NULL,
	"from" bigint NULL,
	change bigint NULL,
	"timestamp" timestamp NOT NULL DEFAULT current_timestamp
);

CREATE INDEX ON transactions("to");
CREATE INDEX ON transactions("from");

