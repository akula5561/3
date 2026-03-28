-- Если Hibernate успел добавить колонки users как nullable (без default), доводим до методички.
do $$
begin
    if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'users' and column_name = 'is_account_expired') then
        update users set is_account_expired = coalesce(is_account_expired, false);
        alter table users alter column is_account_expired set not null;
    end if;
    if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'users' and column_name = 'is_account_locked') then
        update users set is_account_locked = coalesce(is_account_locked, false);
        alter table users alter column is_account_locked set not null;
    end if;
    if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'users' and column_name = 'is_credentials_expired') then
        update users set is_credentials_expired = coalesce(is_credentials_expired, false);
        alter table users alter column is_credentials_expired set not null;
    end if;
    if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'users' and column_name = 'is_disabled') then
        update users set is_disabled = coalesce(is_disabled, false);
        alter table users alter column is_disabled set not null;
    end if;
    if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'users' and column_name = 'name') then
        update users set name = coalesce(nullif(trim(name), ''), username) where name is null or btrim(name) = '';
        alter table users alter column name set not null;
    end if;
    if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'users' and column_name = 'email') then
        update users set email = coalesce(nullif(trim(email), ''), concat('user_', id::text, '@migrated.local')) where email is null or btrim(email) = '';
        alter table users alter column email set not null;
    end if;
    if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'users' and column_name = 'password_hash') then
        alter table users alter column password_hash set not null;
    end if;
end $$;
