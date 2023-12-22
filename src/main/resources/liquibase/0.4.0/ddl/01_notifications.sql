--liquibase formatted sql

--changeset illine:0.4.0/ddl/notifications

--rollback alter table drinking_ponies.notifications alter column created type timestamp(0) with time zone using created::timestamp(0) with time zone;
alter table drinking_ponies.notifications
    alter column created type timestamp(0) using created::timestamp(0);

--rollback alter table drinking_ponies.notifications alter column updated type timestamp(0) with time zone using updated::timestamp(0) with time zone;
alter table drinking_ponies.notifications
    alter column updated type timestamp(0) using updated::timestamp(0);

--rollback alter table drinking_ponies.notifications alter column time_of_last_notification type timestamp(0) with time zone using time_of_last_notification::timestamp(0) with time zone;
alter table drinking_ponies.notifications
    alter column time_of_last_notification type timestamp(0) using time_of_last_notification::timestamp(0);