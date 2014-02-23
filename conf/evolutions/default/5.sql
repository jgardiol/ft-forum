# Add tables to store player information pulled from armory

# --- !Ups

CREATE SEQUENCE player_info_seq;

CREATE TABLE player_info (
	id integer NOT NULL DEFAULT nextval('player_info_seq'),
	name varchar(255) NOT NULL,
	guild varchar(255) NOT NULL,
	PRIMARY KEY(id)
);

# --- !Downs

DROP TABLE player_info IF EXISTS;
DROP SEQUENCE player_info_seq IF EXISTS;