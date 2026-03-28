-- Раньше таблицы users / user_sessions создавал Hibernate; для чистой БД через Flyway они нужны до V4 (FK на users).

create table if not exists users (
    id         bigserial primary key,
    username   varchar(255) not null unique,
    password   varchar(255) not null,
    role       varchar(255) not null default 'USER'
);

create table if not exists user_sessions (
    id           bigserial primary key,
    user_id      bigint       not null references users (id) on delete cascade,
    refresh_jti  varchar(80) unique,
    status       varchar(16)  not null,
    expires_at   timestamptz  not null,
    created_at   timestamptz  not null,
    rotated_at   timestamptz
);

create index if not exists ix_user_sessions_user on user_sessions (user_id);
