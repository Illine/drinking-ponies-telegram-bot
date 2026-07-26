insert into telegram_users (external_user_id, user_time_zone, created, deleted)
values (1, 'Europe/Moscow', now(), false);

insert into telegram_users (external_user_id, user_time_zone, created, deleted)
values (2, 'Europe/Moscow', now(), false);

-- A timezone of its own, to tell the stored owner apart from the one a test DTO carries.
insert into telegram_users (external_user_id, user_time_zone, created, deleted)
values (3, 'Asia/Kolkata', now(), false);
