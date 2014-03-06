# --- !Ups

ALTER TABLE dps DROP COLUMN is_healer;
ALTER TABLE dps ADD COLUMN role integer NOT NULL DEFAULT '0';

UPDATE dps SET role='1' 
WHERE spec='Restoration 204'
OR spec='Holy 501'
OR spec='Discipline 601'
OR spec='Holy 602'
OR spec='Restoration 803'
OR spec='Mistweaver 1102';

UPDATE dps SET role='2'
WHERE spec='Blood 101' 
OR spec='Feral/Bear 203' 
OR spec='Protection 502' 
OR spec='Protection 1003' 
OR spec='Brewmaster 1101';

# --- !Downs

ALTER TABLE dps DROP COLUMN role;
ALTER TABLE dps ADD COLUMN is_healer boolean NOT NULL DEFAULT 'false';