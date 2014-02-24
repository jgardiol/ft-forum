# Crawl information

# --- !Ups

CREATE SEQUENCE crawl_info_seq;

CREATE TABLE crawl_info (
	id integer NOT NULL DEFAULT nextval('crawl_info_seq'),
	last timestamp NOT NULL,
	PRIMARY KEY(id)
);

# --- !Downs

DROP TABLE crawl_info IF EXISTS;
DROP SEQUENCE crawl_info_seq IF EXISTS;