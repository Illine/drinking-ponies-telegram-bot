insert into telegram_users (external_user_id, user_time_zone, created, deleted)
values (1, 'Europe/Moscow', now(), false);

insert into telegram_users (external_user_id, user_time_zone, created, deleted)
values (2, 'Europe/Moscow', now(), false);

insert into telegram_chats (telegram_user_id, external_chat_id)
values (1, 1);

insert into telegram_chats (telegram_user_id, external_chat_id)
values (2, 2);

insert into notification_settings (telegram_user_id, telegram_chat_id, notification_interval, time_of_last_notification, notification_attempts, enabled)
values (1, 1, 'TWO_HOURS', now(), 1, true);

insert into notification_settings (telegram_user_id, telegram_chat_id, notification_interval, time_of_last_notification, notification_attempts, enabled)
values (2, 2, 'TWO_HOURS', now(), 1, false);

-- An admin soft deleted this one while their chat and settings stayed behind. Reading the user
-- through the lazy association blows up on @SQLRestriction, so the mailing query has to join
-- instead - which also drops this user out of the result, and that is the wanted behaviour.
insert into telegram_users (external_user_id, user_time_zone, created, deleted)
values (3, 'Europe/Moscow', now(), true);

insert into telegram_chats (telegram_user_id, external_chat_id)
values (3, 3);

insert into notification_settings (telegram_user_id, telegram_chat_id, notification_interval, time_of_last_notification, notification_attempts, enabled)
values (3, 3, 'TWO_HOURS', now(), 1, true);
