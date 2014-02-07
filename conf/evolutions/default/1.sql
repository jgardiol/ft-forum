# Forums schema

# --- !Ups

CREATE sequence forum_id_seq;

CREATE TABLE forums (
	id integer NOT NULL DEFAULT nextval('forum_id_seq'),
	name varchar(255) NOT NULL,
	description varchar(255),
	PRIMARY KEY(id)
);

CREATE SEQUENCE user_id_seq;

CREATE TABLE users (
	id integer NOT NULL DEFAULT nextval('user_id_seq'),
	name varchar(255) NOT NULL,
	password varchar(255) NOT NULL,
	main varchar(255),
	role varchar(255) NOT NULL,
	PRIMARY KEY(id)
);

INSERT INTO users VALUES ('0', '[DELETED]', 'password', '', '[deleted]');

CREATE SEQUENCE thread_id_seq;

CREATE TABLE threads (
	id integer NOT NULL DEFAULT nextval('thread_id_seq'),
	title varchar(255) NOT NULL,
	forum_id integer NOT NULL,
	user_id integer DEFAULT 0 NOT NULL,
	FOREIGN KEY(forum_id) REFERENCES forums(id) ON DELETE CASCADE,
	FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE SET DEFAULT,
	PRIMARY KEY(id)
);

CREATE SEQUENCE post_id_seq;

CREATE TABLE posts (
	id integer NOT NULL DEFAULT nextval('post_id_seq'),
	content text,
	created timestamp,
	thread_id integer NOT NULL,
	user_id integer NOT NULL,
	foreign key(thread_id) references threads(id) ON DELETE CASCADE,
	FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE SET DEFAULT,
	PRIMARY KEY(id)
);

# --- !Downs

DROP TABLE forums IF EXISTS;
DROP SEQUENCE forum_id_seq IF EXISTS;
DROP TABLE posts IF EXISTS;
DROP SEQUENCE post_id_seq IF EXISTS;
DROP TABLE threads IF EXISTS;
DROP SEQUENCE thread_id_seq IF EXISTS;
DROP TABLE users IF EXISTS;
DROP SEQUENCE user_id_seq IF EXISTS;
