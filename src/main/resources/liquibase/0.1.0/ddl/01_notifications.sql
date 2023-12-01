--liquibase formatted sql

--changeset illine:0.1.0/ddl/notification_seq
--rollback drop sequence drinking_ponies.notification_seq;
create sequence drinking_ponies.notification_seq;

--changeset illine:0.1.0/ddl/notifications
--rollback drop table drinking_ponies.notifications;
create table drinking_ponies.notifications
(
    id                        bigint                      default nextval('drinking_ponies.notification_seq') not null
        constraint notifications_pk primary key,
    user_id                   bigint                                                                          not null,
    chat_id                   bigint                                                                          not null,
    delay_notification        text                                                                            not null,
    time_of_last_notification timestamp(0) with time zone                                                     not null,
    created                   timestamp(0) with time zone default now()                                       not null,
    updated                   timestamp(0) with time zone                                                     not null,
    deleted                   boolean                     default false                                       not null
);

create unique index notifications_user_id_unique_index
    on drinking_ponies.notifications (user_id);

comment on table drinking_ponies.notifications is 'Table storages information for notify users';
comment on column drinking_ponies.notifications.id is 'Primary key of the table';
comment on column drinking_ponies.notifications.user_id is 'ID of Telegram user, it has to be unique';
comment on column drinking_ponies.notifications.chat_id is 'ID of a Telegram chat for notifications';
comment on column drinking_ponies.notifications.delay_notification is 'After how much time should the user receive notifications';
comment on column drinking_ponies.notifications.time_of_last_notification is 'Time when the last notification was sent';
comment on column drinking_ponies.notifications.created is 'Time when the record was created';
comment on column drinking_ponies.notifications.updated is 'Time when the record was updated';
comment on column drinking_ponies.notifications.deleted is 'Flag of removing the record';