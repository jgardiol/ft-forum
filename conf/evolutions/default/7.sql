# Add a column to seperate dps and hps in rankings

# --- !Ups

ALTER TABLE dps ADD COLUMN is_healer boolean NOT NULL DEFAULT 'false';

UPDATE TABLE dps SET is_healer='true' 
WHERE spec='Restoration 204'
OR spec='Holy 501'
OR spec='Discipline 601'
OR spec='Holy 602'
OR spec='Restoration 803'
OR spec='Mistweaver 1102';

# --- !Downs

ALTER TABLE dps DROP COLUMN is_healer IF EXISTS;