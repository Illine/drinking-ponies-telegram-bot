--liquibase formatted sql

--changeset illine:7.4.0/ddl/grants
--rollback revoke all on table telegram_users, telegram_chats, notification_settings from dptb;

grant all on table telegram_users to dptb;
grant all on table telegram_chats to dptb;
grant all on table notification_settings to dptb;

grant usage, select, update on sequence telegram_user_seq to dptb;
grant usage, select, update on sequence telegram_chat_seq to dptb;
grant usage, select, update on sequence notification_setting_seq to dptb;
