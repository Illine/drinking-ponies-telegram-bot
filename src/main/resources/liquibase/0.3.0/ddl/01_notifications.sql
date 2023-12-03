--liquibase formatted sql

--changeset illine:0.3.0/ddl/notification_seq
--rollback alter table drinking_ponies.notifications drop column notification_attempts;
alter table drinking_ponies.notifications
    add notification_attempts integer default 0 not null;
comment on column drinking_ponies.notifications.notification_attempts is 'The number of notification attempts made';

--rollback alter table drinking_ponies.notifications drop column previous_notification_message_id;
alter table drinking_ponies.notifications
    add previous_notification_message_id integer;
comment on column drinking_ponies.notifications.previous_notification_message_id is 'The id of previous notification message';