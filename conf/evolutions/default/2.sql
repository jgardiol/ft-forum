# Forums schema

# --- !Ups


CREATE SEQUENCE boss_id_seq;

CREATE TABLE bosses (
	id integer NOT NULL DEFAULT nextval('boss_id_seq'),
	name varchar(255) NOT NULL,
	difficulty varchar(255) NOT NULL,
	PRIMARY KEY(id)
);

CREATE SEQUENCE dps_seq_id;

CREATE TABLE dps (
	id integer NOT NULL DEFAULT nextval('dps_seq_id'),
	value double precision NOT NULL,
	spec varchar(255) NOT NULL,
	report_id varchar(255) NOT NULL,
	player_name varchar(255) NOT NULL,
	boss_id integer DEFAULT 0 NOT NULL,
	FOREIGN KEY(boss_id) REFERENCES bosses(id) ON DELETE CASCADE,
	PRIMARY KEY(id)
);

CREATE SEQUENCE guild_id_seq;

CREATE TABLE guilds (
	id integer NOT NULL DEFAULT nextval('guild_id_seq'),
	name varchar(255),
	wol_id varchar(255),
	last_report timestamp,
	PRIMARY KEY(id)
);

# --- !Downs

DROP TABLE guilds IF EXISTS;
DROP SEQUENCE guild_id_seq IF EXISTS;
DROP TABLE dps IF EXISTS;
DROP SEQUENCE dps_seq_id IF EXISTS;
DROP TABLE bosses IF EXISTS;
DROP SEQUENCE boss_id_seq IF EXISTS;
