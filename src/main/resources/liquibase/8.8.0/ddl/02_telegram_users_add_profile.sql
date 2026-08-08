--liquibase formatted sql

--changeset illine:8.8.0/ddl/telegram_users_add_profile
/* liquibase rollback
 alter table telegram_users drop column first_name;
 alter table telegram_users drop column last_name;
 alter table telegram_users drop column username;
 alter table telegram_users drop column avatar_url;
*/

alter table telegram_users
    add column first_name text null,
    add column last_name  text null,
    add column username   text null,
    add column avatar_url  text null;

comment on column telegram_users.first_name is 'Telegram first name, refreshed on user activity';
comment on column telegram_users.last_name is 'Telegram last name, refreshed on user activity; null if not set';
comment on column telegram_users.username is 'Telegram @username without the leading @, refreshed on user activity; null if not set';
comment on column telegram_users.avatar_url is 'Avatar URL taken from MiniApp initData; null for users who never opened the MiniApp';
