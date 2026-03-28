-- До первой активации поле user_id в методичке = NULL (владелец задаётся через owner_id).
alter table licenses alter column user_id drop not null;
