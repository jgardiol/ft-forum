# Forums schema

# --- !Ups


CREATE SEQUENCE crawl_id_seq;

CREATE TABLE crawl_errors (
	id integer NOT NULL DEFAULT nextval('crawl_id_seq'),
	message varchar(255) NOT NULL,
	url varchar(255) NOT NULL,
	PRIMARY KEY(id)
);

# --- !Downs

DROP TABLE crawl_errors IF EXISTS;
DROP SEQUENCE crawl_id_seq IF EXISTS;
