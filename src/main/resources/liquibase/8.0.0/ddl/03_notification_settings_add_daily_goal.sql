--liquibase formatted sql

--changeset illine:8.0.0/ddl/notification_settings_add_daily_goal
--rollback alter table notification_settings drop column daily_goal_ml;

alter table notification_settings
    add column daily_goal_ml integer not null default 2000;

comment on column notification_settings.daily_goal_ml is 'Daily water intake goal in milliliters; default 2000ml';
