--liquibase formatted sql

--changeset illine:8.8.0/ddl/state_comments_without_value_lists

/* liquibase rollback
 comment on column user_state_events.event_type is 'Type of the event: BANNED, UNBANNED, DEACTIVATED, RESTORED';
 comment on column telegram_users.is_admin is 'Whether the user has admin privileges; bootstrapped manually via SQL after deploy';
 comment on column telegram_users.is_banned is 'Whether the user has been banned; bootstrapped manually via SQL after deploy';
*/

comment on column user_state_events.event_type is 'Type of the state change, one of the UserStateEventType values';

comment on column telegram_users.is_admin is 'Whether the user has admin privileges; the first one is bootstrapped manually via SQL, the rest through the admin API';

comment on column telegram_users.is_banned is 'Whether the user has been banned; set through the admin API';
