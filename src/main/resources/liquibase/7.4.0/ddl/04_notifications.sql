--liquibase formatted sql

--changeset illine:7.4.0/ddl/notifications

/* liquibase rollback
 insert into notifications (
    user_id,
    chat_id,
    delay_notification,
    time_of_last_notification,
    created,
    updated,
    deleted,
    notification_attempts,
    previous_notification_message_id,
    user_time_zone,
    quiet_mode_start,
    quiet_mode_end
 )
 select
    u.external_user_id                        as user_id,
    tc.external_chat_id                       as chat_id,
    ns.delay_notification,
    ns.time_of_last_notification,
    u.created                                 as created,
    ns.time_of_last_notification              as updated,
    not ns.enabled                            as deleted,
    ns.notification_attempts,
    tc.previous_notification_external_message_id,
    u.user_time_zone,
    ns.quiet_mode_start,
    ns.quiet_mode_end
 from notification_settings ns
 join telegram_chats tc
   on tc.id = ns.telegram_chat_id
 join telegram_users u
   on u.id = ns.telegram_user_id;
*/

insert into telegram_users (external_user_id, user_time_zone, created)
select distinct on (n.user_id)
    n.user_id        as external_user_id,
    n.user_time_zone as user_time_zone,
    n.created        as created
from notifications n
order by n.user_id, n.created desc;

insert into telegram_chats (telegram_user_id, external_chat_id, previous_notification_external_message_id)
select distinct on (u.id, n.chat_id)
    u.id                              as telegram_user_id,
    n.chat_id                         as external_chat_id,
    n.previous_notification_message_id as previous_notification_external_message_id
from notifications n
         join telegram_users u
              on u.external_user_id = n.user_id
order by u.id, n.chat_id, n.time_of_last_notification desc nulls last;

insert into notification_settings (
    telegram_user_id,
    telegram_chat_id,
    delay_notification,
    time_of_last_notification,
    notification_attempts,
    quiet_mode_start,
    quiet_mode_end,
    enabled
)
select distinct on (u.id, tc.id)
    u.id          as telegram_user_id,
    tc.id         as telegram_chat_id,
    n.delay_notification,
    n.time_of_last_notification,
    n.notification_attempts,
    n.quiet_mode_start,
    n.quiet_mode_end,
    not n.deleted as enabled
from notifications n
         join telegram_users u
              on u.external_user_id = n.user_id
         join telegram_chats tc
              on tc.telegram_user_id = u.id
                  and tc.external_chat_id = n.chat_id
order by u.id, tc.id, n.time_of_last_notification desc nulls last;

delete from notifications;
