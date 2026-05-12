--liquibase formatted sql

--changeset illine:8.3.0/ddl/water_statistics_add_source
--rollback alter table water_statistics drop column source;

alter table water_statistics
    add column source varchar(32) not null default 'NOTIFICATION';

comment on column water_statistics.source is 'Source of the water entry: NOTIFICATION = recorded via Telegram callback, MANUAL = recorded via MiniApp manual entry';
