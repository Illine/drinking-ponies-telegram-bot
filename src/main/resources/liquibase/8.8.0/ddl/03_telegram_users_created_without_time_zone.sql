--liquibase formatted sql

--changeset illine:8.8.0/ddl/telegram_users_created_without_time_zone
/* liquibase rollback
 alter table telegram_users
     alter column created type timestamp(0) with time zone using created at time zone 'UTC',
     alter column created set default now();
*/

alter table telegram_users
    alter column created type timestamp(0) using created at time zone 'UTC',
    alter column created set default (now() at time zone 'UTC');

comment on column telegram_users.created is 'Time when the record was created, stored in UTC';
