-- Удаление демо-домена библиотеки (книги, читатели, выдачи). Остаётся модуль лицензий (V4) и users из JPA.
drop table if exists loans cascade;
drop table if exists books cascade;
drop table if exists authors cascade;
drop table if exists readers cascade;
