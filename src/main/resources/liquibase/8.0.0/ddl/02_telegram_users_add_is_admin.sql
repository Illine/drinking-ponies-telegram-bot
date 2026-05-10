--liquibase formatted sql

--changeset illine:8.0.0/ddl/telegram_users_add_is_admin
--rollback alter table telegram_users drop column is_admin;

alter table telegram_users
    add column is_admin boolean not null default false;

comment on column telegram_users.is_admin is 'Whether the user has admin privileges; bootstrapped manually via SQL after deploy';
