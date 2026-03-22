--liquibase formatted sql

--changeset illine:7.6.0/ddl/drop_notifications
--rollback create sequence notification_seq;
--rollback create table notifications (id bigint default nextval('notification_seq') not null constraint notifications_pk primary key, user_id bigint not null, chat_id bigint not null, delay_notification interval, time_of_last_notification timestamp(0), created timestamp(0), updated timestamp(0), deleted boolean default false not null, notification_attempts integer default 0 not null, previous_notification_message_id integer, user_time_zone text default 'Europe/Moscow' not null, quiet_mode_start time, quiet_mode_end time);
--rollback create unique index notifications_user_id_unique_index on notifications (user_id);

drop table notifications;
drop sequence notification_seq;