# Add a column to seperate dps and hps in rankings

# --- !Ups

ALTER TABLE dps ADD COLUMN is_healer boolean NOT NULL DEFAULT 'false';

# --- !Downs

ALTER TABLE dps DROP COLUMN is_healer IF EXISTS;