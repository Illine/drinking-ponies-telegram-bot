--liquibase formatted sql

--changeset illine:8.0.0/ddl/notification_settings_add_pause_until
--rollback alter table notification_settings drop column pause_until;

alter table notification_settings
    add column pause_until timestamp(0) null;

comment on column notification_settings.pause_until is 'UTC timestamp until which notifications are paused, null means not paused';
