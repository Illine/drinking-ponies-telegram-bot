--liquibase formatted sql

--changeset illine:7.9.0/ddl/create_necessary_indexes

/* liquibase rollback
 drop index notification_settings_telegram_user_id_unique_index;
 drop index water_statistics_user_id_event_time_index;
*/

create unique index notification_settings_telegram_user_id_unique_index
    on notification_settings (telegram_user_id);

create index water_statistics_user_id_event_time_index
    on water_statistics (user_id, event_time);