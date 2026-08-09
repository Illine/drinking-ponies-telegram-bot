--liquibase formatted sql

--changeset illine:8.8.0/ddl/user_state_events

/* liquibase rollback
 drop table user_state_events;
 drop sequence user_state_event_seq;
*/

create sequence user_state_event_seq;

create table user_state_events
(
    id            bigint default nextval('user_state_event_seq') not null
        primary key,
    user_id       bigint                                         not null
        references telegram_users (id),
    actor_user_id bigint                                         not null
        references telegram_users (id),
    event_type    text                                           not null,
    event_time    timestamp(0)                                   not null
);

comment on table user_state_events is 'History of account state changes; the current state itself lives in telegram_users';
comment on column user_state_events.id is 'Primary key of the table';
comment on column user_state_events.user_id is 'Whose state has changed';
comment on column user_state_events.actor_user_id is 'Who changed it: an admin or the user themselves';
comment on column user_state_events.event_type is 'Type of the event: BANNED, UNBANNED, DEACTIVATED, RESTORED';
comment on column user_state_events.event_time is 'Time when the change was applied, stored in UTC';
