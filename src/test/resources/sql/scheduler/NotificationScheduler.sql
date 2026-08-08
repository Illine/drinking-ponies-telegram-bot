-- Two users whose reminders are long overdue against the fixed test clock, so the scheduler is
-- bound to pick them up. The soft-deleted one is inserted first on purpose: that is the order the
-- mailing walks them in, so a failure on the hidden row happens before anyone is notified - which
-- is exactly how one deleted account used to silence the reminders for everybody.
insert into telegram_users (external_user_id, user_time_zone, created, deleted)
values (1, 'Europe/Moscow', now(), true);

insert into telegram_users (external_user_id, user_time_zone, created, deleted)
values (2, 'Europe/Moscow', now(), false);

insert into telegram_chats (telegram_user_id, external_chat_id)
values (1, 1);

insert into telegram_chats (telegram_user_id, external_chat_id)
values (2, 2);

-- No quiet mode and zero attempts, so nothing else filters them out of the batch.
insert into notification_settings (telegram_user_id, telegram_chat_id, notification_interval, time_of_last_notification, notification_attempts, enabled)
values (1, 1, 'TWO_HOURS', timestamp '2024-01-01 00:00:00', 0, true);

insert into notification_settings (telegram_user_id, telegram_chat_id, notification_interval, time_of_last_notification, notification_attempts, enabled)
values (2, 2, 'TWO_HOURS', timestamp '2024-01-01 00:00:00', 0, true);
