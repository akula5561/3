-- Возьмём первую попавшуюся книгу и читателя
-- и создадим одну "активную" просроченную выдачу: due_date = сегодня - 3 дня

insert into loans (book_id, reader_id, loan_date, due_date, return_date)
select
    b.id,
    r.id,
    current_date - interval '10 day',    -- выдали 10 дней назад
    current_date - interval '3 day',     -- срок сдачи был 3 дня назад -> просрочка
    null
from books b
    cross join readers r
where not exists (
    select 1 from loans l where l.book_id = b.id and l.return_date is null
    )
    limit 1;

-- Для наглядности можно продублировать на другую книгу/читателя (если есть)
insert into loans (book_id, reader_id, loan_date, due_date, return_date)
select
    b.id,
    r.id,
    current_date - interval '20 day',
    current_date - interval '5 day',
    null
from books b
    cross join readers r
where b.id not in (select book_id from loans where return_date is null)
  and r.id not in (select reader_id from loans where return_date is null)
    limit 1;
