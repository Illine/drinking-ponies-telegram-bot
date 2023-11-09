--liquibase formatted sql

--changeset illine:0.0.1/ddl/user_notification_seq
--rollback drop sequence drinking_ponies.user_notification_seq;
create sequence drinking_ponies.user_notification_seq;

--changeset illine:0.0.1/ddl/user_notifications
--rollback drop table drinking_ponies.user_notifications;
create table drinking_ponies.user_notifications
(
    id                        bigint                      default nextval('drinking_ponies.user_notification_seq') not null
        constraint user_notifications_pk primary key,
    user_id                   bigint                                                                               not null,
    username                  varchar(32)                                                                          not null,
    first_name                text,
    last_name                 text,
    language_code             text,
    premium                   boolean                                                                              not null,
    chat_id                   bigint                                                                               not null,
    delay_notification        text                                                                                 not null,
    time_of_last_notification timestamp(0) with time zone                                                          not null,
    created                   timestamp(0) with time zone default now()                                            not null,
    updated                   timestamp(0) with time zone                                                          not null,
    deleted                   boolean                     default false                                            not null
);

create unique index user_notifications_user_id_unique_index
    on drinking_ponies.user_notifications (user_id);

comment on table drinking_ponies.user_notifications is 'Table storages information for notify users';
comment on column drinking_ponies.user_notifications.id is 'Primary key of the table';
comment on column drinking_ponies.user_notifications.user_id is 'ID of Telegram user, it has to be unique';
comment on column drinking_ponies.user_notifications.username is 'Username from Telegram';
comment on column drinking_ponies.user_notifications.first_name is 'First name of Telegram user, it can be null';
comment on column drinking_ponies.user_notifications.last_name is 'Last name of Telegram user, it can be null';
comment on column drinking_ponies.user_notifications.language_code is 'Language code of Telegram user, it can be null';
comment on column drinking_ponies.user_notifications.premium is 'Flag of premium of Telegram user, it has default value as false';
comment on column drinking_ponies.user_notifications.chat_id is 'ID of a Telegram chat for notifications';
comment on column drinking_ponies.user_notifications.delay_notification is 'After how much time should the user receive notifications';
comment on column drinking_ponies.user_notifications.time_of_last_notification is 'Time when the last notification was sent';
comment on column drinking_ponies.user_notifications.created is 'Time when the record was created';
comment on column drinking_ponies.user_notifications.updated is 'Time when the record was updated';
comment on column drinking_ponies.user_notifications.deleted is 'Flag of removing the record';