-- Справочники для лабы (продукты и типы лицензий). Фиксированные UUID — те же в PostgresDemoDataSeed.

insert into products (id, name, is_blocked)
values ('11111111-1111-1111-1111-111111111101'::uuid, 'Demo Antivirus', false)
on conflict (id) do nothing;

insert into license_types (id, name, default_duration_in_days, description)
values
    ('22222222-2222-2222-2222-222222222201'::uuid, 'TRIAL', 14, 'Пробный период'),
    ('22222222-2222-2222-2222-222222222202'::uuid, 'YEAR', 365, 'Годовая лицензия')
on conflict (id) do nothing;
