# Forums schema

# --- !Ups

CREATE SEQUENCE role_id_seq;

CREATE TABLE roles (
	id integer NOT NULL DEFAULT nextval('role_id_seq'),
	title varchar(255) NOT NULL,
	is_admin boolean NOT NULL,
	PRIMARY KEY(id)
);

CREATE TABLE permissions (
	role_id integer NOT NULL,
	forum_id integer NOT NULL,
	permission integer NOT NULL,
	FOREIGN KEY(role_id) REFERENCES roles(id) ON DELETE CASCADE,
	FOREIGN KEY(forum_id) REFERENCES forums(id) ON DELETE CASCADE,
	PRIMARY KEY(role_id, forum_id)
);

ALTER TABLE users DROP COLUMN role;
ALTER TABLE users ADD COLUMN role_id integer;
ALTER TABLE users ADD CONSTRAINT rolefk FOREIGN KEY(role_id) REFERENCES roles(id) ON DELETE SET NULL;

INSERT INTO roles VALUES('-2', 'visiteur', 'false');
INSERT INTO roles VALUES('-1', 'newbie', 'false');
INSERT INTO roles VALUES('0', 'admin', 'true');

ALTER TABLE posts ADD COLUMN modified timestamp;
ALTER TABLE posts ADD COLUMN modified_by integer;
ALTER TABLE posts ADD CONSTRAINT modifiedfk FOREIGN KEY(modified_by) REFERENCES users(id) ON DELETE SET NULL;

# --- !Downs

DROP TABLE permissions IF EXISTS;
DROP TABLE roles IF EXISTS;
DROP SEQUENCE role_id_seq IF EXISTS;
