# --- !Ups

ALTER TABLE dps DROP COLUMN is_healer;

# --- !Downs

ALTER TABLE dps ADD COLUMN is_healer boolean NOT NULL DEFAULT 'false';