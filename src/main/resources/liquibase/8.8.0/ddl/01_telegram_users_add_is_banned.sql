--liquibase formatted sql

--changeset illine:8.8.0/ddl/telegram_users_add_is_banned
--rollback alter table telegram_users drop column is_banned;

alter table telegram_users
    add column is_banned boolean not null default false;

comment on column telegram_users.is_banned is 'Whether the user has been banned; bootstrapped manually via SQL after deploy';
