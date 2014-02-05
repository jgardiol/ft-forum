# Forums schema

# --- !Ups

SET IGNORECASE TRUE;

CREATE sequence forum_id_seq;

CREATE TABLE forum (
	id integer NOT NULL DEFAULT nextval('forum_id_seq'),
	name varchar(255) NOT NULL,
	description varchar(255)
);

CREATE SEQUENCE user_id_seq;

CREATE TABLE user (
	id integer NOT NULL DEFAULT nextval('user_id_seq'),
	name varchar(255) NOT NULL,
	password varchar(255) NOT NULL,
	main varchar(255),
	role varchar(255) NOT NULL
);

INSERT INTO user VALUES ('0', '[DELETED]', 'password', '', '[deleted]');

CREATE SEQUENCE thread_id_seq;

CREATE TABLE thread (
	id integer NOT NULL DEFAULT nextval('thread_id_seq'),
	title varchar(255) NOT NULL,
	forum_id integer NOT NULL,
	user_id integer DEFAULT 0 NOT NULL,
	FOREIGN KEY(forum_id) REFERENCES forum(id) ON DELETE CASCADE,
	FOREIGN KEY(user_id) REFERENCES user(id) ON DELETE SET DEFAULT
);

CREATE SEQUENCE post_id_seq;

CREATE TABLE post (
	id integer NOT NULL DEFAULT nextval('post_id_seq'),
	content clob,
	created timestamp,
	thread_id integer NOT NULL,
	user_id integer NOT NULL,
	foreign key(thread_id) references thread(id) ON DELETE CASCADE,
	FOREIGN KEY(user_id) REFERENCES user(id) ON DELETE SET DEFAULT
);

# --- !Downs

DROP TABLE forum IF EXISTS;
DROP SEQUENCE forum_id_seq IF EXISTS;
DROP TABLE post IF EXISTS;
DROP SEQUENCE post_id_seq IF EXISTS;
DROP TABLE thread IF EXISTS;
DROP SEQUENCE thread_id_seq IF EXISTS;
DROP TABLE user IF EXISTS;
DROP SEQUENCE user_id_seq IF EXISTS;
