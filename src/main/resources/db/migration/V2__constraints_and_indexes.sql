-- Уникальность и индексы для домена библиотеки (таблицы удаляет V5).
-- Важно: не использовать $$ внутри тела do $tag$ ... $tag$ — иначе закроется строка.

do $v2$
begin
    if not exists (
        select 1 from information_schema.table_constraints
        where constraint_schema = 'public'
          and constraint_type = 'UNIQUE'
          and table_name = 'authors'
          and constraint_name = 'uq_authors_name'
    ) then
        alter table authors add constraint uq_authors_name unique (name);
    end if;
exception
    when others then
        null;
end $v2$;

do $v2$
begin
    if not exists (
        select 1 from information_schema.table_constraints
        where constraint_schema = 'public'
          and constraint_type = 'UNIQUE'
          and table_name = 'readers'
          and constraint_name = 'uq_readers_email'
    ) then
        alter table readers add constraint uq_readers_email unique (email);
    end if;
exception
    when others then
        null;
end $v2$;

create index if not exists idx_books_author on books (author_id);
create index if not exists idx_loans_reader on loans (reader_id);
create index if not exists idx_loans_book on loans (book_id);

create unique index if not exists uq_loans_book_active
    on loans (book_id)
    where return_date is null;
