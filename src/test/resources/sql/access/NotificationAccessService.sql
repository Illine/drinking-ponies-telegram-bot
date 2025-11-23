insert into notifications (user_id, chat_id, delay_notification,
                                           time_of_last_notification,
                                           notification_attempts,
                                           created,
                                           updated,
                                           user_time_zone)
values (1, 1, 'TWO_HOURS', now(), 1, now(), now(), 'Europe/Moscow');

insert into notifications (user_id, chat_id, delay_notification,
                                           time_of_last_notification,
                                           notification_attempts,
                                           created,
                                           updated,
                                           user_time_zone,
                                           deleted)
values (2, 2, 'TWO_HOURS', now(), 1, now(), now(), 'Europe/Moscow', true);