--liquibase formatted sql

--changeset illine:7.4.0/ddl/users

/* liquibase rollback
 drop table telegram_users;
 drop sequence telegram_user_seq;
*/

create sequence telegram_user_seq;

create table telegram_users
(
    id               bigint default nextval('telegram_user_seq') not null
        primary key,
    external_user_id bigint                                   not null,
    user_time_zone   text                        default 'Europe/Moscow'::text not null,
    created          timestamp(0) with time zone default now()                 not null,
    deleted          boolean                     default false                 not null
);

create unique index telegram_users_external_user_id_unique_index
    on telegram_users (external_user_id);

comment on table telegram_users is 'Table storing information about telegram users';
comment on column telegram_users.id is 'Primary key of the table';
comment on column telegram_users.external_user_id is 'ID of a Telegram user, it has to be unique';
comment on column telegram_users.user_time_zone is 'Timezone of the user, default Europe/Moscow';
comment on column telegram_users.created is 'Time when the record was created';
comment on column telegram_users.deleted is 'Soft delete flag of the record';
