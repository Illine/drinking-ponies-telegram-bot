--liquibase formatted sql

--changeset illine:0.4.0/ddl/notifications

--rollback alter table notifications drop column quiet_mode_start;
alter table notifications
    add column quiet_mode_start time;

--rollback alter table notifications drop column quiet_mode_end;
alter table notifications
    add column quiet_mode_end time;