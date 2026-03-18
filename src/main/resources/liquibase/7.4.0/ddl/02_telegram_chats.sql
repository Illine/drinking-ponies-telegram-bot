--liquibase formatted sql

--changeset illine:7.4.0/ddl/telegram_chats

/* liquibase rollback
 drop table telegram_chats;
 drop sequence telegram_chat_seq;
*/

create sequence telegram_chat_seq;

create table telegram_chats
(
    id                                        bigint default nextval('telegram_chat_seq') not null
        primary key,
    telegram_user_id bigint not null
        references telegram_users (id),
    external_chat_id bigint not null,
    previous_notification_external_message_id integer
);

create unique index telegram_chats_external_chat_id_unique_index
    on telegram_chats (external_chat_id);

comment on table telegram_chats is 'Table storing information about Telegram chats';
comment on column telegram_chats.id is 'Primary key of the table';
comment on column telegram_chats.telegram_user_id is 'Foreign key referencing the telegram users table';
comment on column telegram_chats.external_chat_id is 'ID of a Telegram chat for notifications';
comment on column telegram_chats.previous_notification_external_message_id is 'ID of the previous notification Telegram message sent to this chat';
