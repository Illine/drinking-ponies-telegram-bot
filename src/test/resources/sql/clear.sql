delete from water_statistics where id is not null;
delete from notification_settings where id is not null;
delete from telegram_chats where id is not null;
delete from telegram_users where id is not null;

alter sequence water_statistics_seq restart with 1;
alter sequence notification_setting_seq restart with 1;
alter sequence telegram_chat_seq restart with 1;
alter sequence telegram_user_seq restart with 1;
