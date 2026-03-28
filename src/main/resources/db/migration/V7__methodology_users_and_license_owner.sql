-- Методичка: users (name, email, флаги учётки), licenses.owner_id как bigint → users(id).

-- --- users ---
alter table users add column if not exists name varchar(255);
alter table users add column if not exists email varchar(255);
alter table users add column if not exists is_account_expired boolean not null default false;
alter table users add column if not exists is_account_locked boolean not null default false;
alter table users add column if not exists is_credentials_expired boolean not null default false;
alter table users add column if not exists is_disabled boolean not null default false;

update users set name = username where name is null;
update users set email = concat('user_', id::text, '@migrated.local') where email is null;

alter table users alter column name set not null;
alter table users alter column email set not null;

create unique index if not exists uq_users_email on users (email);

-- password → password_hash (как в методичке)
do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'users' and column_name = 'password'
    ) then
        alter table users rename column password to password_hash;
    end if;
end $$;

-- --- licenses.owner_id: uuid → bigint FK ---
do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'licenses'
          and column_name = 'owner_id' and udt_name = 'uuid'
    ) then
        alter table licenses rename column owner_id to legacy_owner_uuid;
    end if;
end $$;

alter table licenses add column if not exists owner_id bigint references users (id) on delete restrict;

update licenses set owner_id = user_id where owner_id is null and user_id is not null;

-- Оставшиеся без владельца — удаляем (данные до выравнивания схемы несовместимы с методичкой)
delete from license_history where license_id in (select id from licenses where owner_id is null);
delete from device_licenses where license_id in (select id from licenses where owner_id is null);
delete from licenses where owner_id is null;

alter table licenses alter column owner_id set not null;

alter table licenses drop column if exists legacy_owner_uuid;
