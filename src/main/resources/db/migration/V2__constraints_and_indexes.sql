/* Уникальность имени автора и email читателя (на случай если не было) */
do $$ begin
  if not exists (select 1 from information_schema.table_constraints
      where constraint_type='UNIQUE' and table_name='authors' and constraint_name='uq_authors_name') then
alter table authors add constraint uq_authors_name unique (name);
end if;
exception when others then
  -- H2 fallback: выполнить простую команду (ошибка do $$ игнорируется в H2)
end $$;

do $$ begin
  if not exists (select 1 from information_schema.table_constraints
      where constraint_type='UNIQUE' and table_name='readers' and constraint_name='uq_readers_email') then
alter table readers add constraint uq_readers_email unique (email);
end if;
exception when others then
end $$;

/* Индексы по внешним ключам, если вдруг отсутствуют */
create index if not exists idx_books_author  on books(author_id);
create index if not exists idx_loans_reader  on loans(reader_id);
create index if not exists idx_loans_book    on loans(book_id);

/* Частичный уникальный индекс: у одной книги только ОДНА активная выдача (return_date is null).
   Поддерживается Postgres и H2 2.x в режиме PostgreSQL. */
create unique index if not exists uq_loans_book_active
    on loans(book_id)
    where return_date is null;
