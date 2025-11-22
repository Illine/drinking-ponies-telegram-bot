--liquibase formatted sql

--changeset illine:0.3.0/ddl/notification_seq
--rollback alter table notifications drop column notification_attempts;
alter table notifications
    add notification_attempts integer default 0 not null;
comment on column notifications.notification_attempts is 'The number of notification attempts made';

--rollback alter table notifications drop column previous_notification_message_id;
alter table notifications
    add previous_notification_message_id integer;
comment on column notifications.previous_notification_message_id is 'The id of previous notification message';

--rollback alter table notifications drop column user_time_zone;
alter table notifications
    add user_time_zone text default 'Europe/Moscow' not null;
comment on column notifications.user_time_zone is 'Timezone of user, default Europe/Moscow';