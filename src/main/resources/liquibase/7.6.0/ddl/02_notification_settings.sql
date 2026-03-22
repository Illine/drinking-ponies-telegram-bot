--liquibase formatted sql

--changeset illine:7.6.0/ddl/rename_delay_notification
--rollback alter table notification_settings rename column notification_interval to delay_notification;

alter table notification_settings rename column delay_notification to notification_interval;
