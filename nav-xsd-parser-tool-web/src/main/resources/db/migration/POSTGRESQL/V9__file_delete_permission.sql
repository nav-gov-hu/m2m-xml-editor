-- Separate permission for physical XML file archiving/deletion.
insert into app_role (role_code, role_name)
select 'FILE_DELETE', 'Fizikai XML állomány archiválás/törlés'
where not exists (select 1 from app_role where role_code = 'FILE_DELETE');
