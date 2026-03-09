-- === Authors ===
create table if not exists authors (
                                       id   bigserial primary key,
                                       name varchar(255) not null unique
    );

-- === Readers ===
create table if not exists readers (
                                       id    bigserial primary key,
                                       name  varchar(255) not null,
    email varchar(255) not null unique
    );

-- === Books ===
create table if not exists books (
                                     id             bigserial primary key,
                                     title          varchar(255) not null,
    published_year int,
    available      boolean not null default true,
    author_id      bigint not null references authors(id) on delete restrict
    );
create index if not exists idx_books_author on books(author_id);

-- === Loans ===
create table if not exists loans (
                                     id          bigserial primary key,
                                     book_id     bigint not null references books(id) on delete restrict,
    reader_id   bigint not null references readers(id) on delete restrict,
    loan_date   date    not null,
    due_date    date    not null,
    return_date date
    );
create index if not exists idx_loans_reader on loans(reader_id);
create index if not exists idx_loans_book on loans(book_id);
create index if not exists idx_loans_active on loans(return_date);

-- Доп. инвариант: у одной книги не более одной активной выдачи (return_date is null)
-- Это частично контролируем сервисом, но можно усилить БД через частичный индекс (PostgreSQL):
create unique index if not exists uq_loans_book_active
    on loans(book_id)
    where return_date is null;

-- === Seed (минимум для проверки) ===
insert into authors(name) values ('Arthur Conan Doyle') on conflict do nothing;
insert into authors(name) values ('J. K. Rowling') on conflict do nothing;

insert into readers(name, email) values ('Ivan Petrov', 'ivan@example.com')
    on conflict (email) do nothing;
insert into readers(name, email) values ('Anna Sidorova', 'anna@example.com')
    on conflict (email) do nothing;

insert into books(title, published_year, available, author_id)
select 'Sherlock Holmes', 1892, true, a.id from authors a where a.name='Arthur Conan Doyle'
    on conflict do nothing;

insert into books(title, published_year, available, author_id)
select 'Harry Potter', 1997, true, a.id from authors a where a.name='J. K. Rowling'
    on conflict do nothing;
