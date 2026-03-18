--liquibase formatted sql

--changeset illine:7.4.0/ddl/notification_settings

/* liquibase rollback
 drop table notification_settings;
 drop sequence notification_setting_seq;
*/

create sequence notification_setting_seq;

create table notification_settings
(
    id                        bigint  default nextval('notification_setting_seq') not null
        primary key,
    telegram_user_id bigint not null
        references telegram_users (id),
    telegram_chat_id          bigint                                               not null
        references telegram_chats (id),
    delay_notification        text                                                 not null,
    time_of_last_notification timestamp(0)                                         not null,
    notification_attempts     integer default 0                                    not null,
    quiet_mode_start          time,
    quiet_mode_end            time,
    enabled                   boolean default false                                not null
);

comment on table notification_settings is 'Table storing notification settings for users';
comment on column notification_settings.id is 'Primary key of the table';
comment on column notification_settings.telegram_user_id is 'Foreign key referencing the telegram users table';
comment on column notification_settings.telegram_chat_id is 'Foreign key referencing the telegram_chats table';
comment on column notification_settings.delay_notification is 'Duration after which the user should receive notifications';
comment on column notification_settings.time_of_last_notification is 'Time when the last notification was sent';
comment on column notification_settings.notification_attempts is 'Number of notification attempts made';
comment on column notification_settings.quiet_mode_start is 'Start time of the quiet mode when notifications should be suppressed';
comment on column notification_settings.quiet_mode_end is 'End time of the quiet mode when notifications are allowed again';
comment on column notification_settings.enabled is 'Flag indicating whether notifications are enabled for this user and chat';
