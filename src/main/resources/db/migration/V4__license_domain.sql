-- === License domain: products, license types, licenses, devices ===

create table if not exists products (
    id          uuid primary key,
    name        varchar(255) not null,
    is_blocked  boolean      not null default false
);

create unique index if not exists uq_products_name on products(name);

create table if not exists license_types (
    id                       uuid primary key,
    name                     varchar(255) not null,
    default_duration_in_days integer      not null,
    description              text
);

create unique index if not exists uq_license_types_name on license_types(name);

create table if not exists licenses (
    id                 uuid primary key,
    code               varchar(255) not null unique,
    user_id            bigint       not null references users (id) on delete restrict,
    product_id         uuid         not null references products (id) on delete restrict,
    type_id            uuid         not null references license_types (id) on delete restrict,
    first_activation_date date,
    ending_date          date,
    blocked              boolean     not null default false,
    device_count         integer     not null,
    owner_id             uuid,
    description          text
);

create index if not exists idx_licenses_user on licenses(user_id);
create index if not exists idx_licenses_product on licenses(product_id);
create index if not exists idx_licenses_type on licenses(type_id);

create table if not exists devices (
    id          uuid primary key,
    name        varchar(255) not null,
    mac_address varchar(255) not null,
    user_id     bigint       not null references users (id) on delete restrict
);

create unique index if not exists uq_devices_mac on devices(mac_address);
create index if not exists idx_devices_user on devices(user_id);

create table if not exists device_licenses (
    id             uuid primary key,
    license_id     uuid   not null references licenses (id) on delete cascade,
    device_id      uuid   not null references devices (id) on delete cascade,
    activation_date date not null
);

create unique index if not exists uq_device_licenses_pair
    on device_licenses(license_id, device_id);

create table if not exists license_history (
    id          uuid primary key,
    license_id  uuid   not null references licenses (id) on delete cascade,
    user_id     bigint not null references users (id) on delete restrict,
    status      varchar(64) not null,
    change_date timestamp with time zone not null,
    description text
);

create index if not exists idx_license_history_license on license_history(license_id);
create index if not exists idx_license_history_user on license_history(user_id);

