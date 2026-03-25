--liquibase formatted sql

--changeset illine:7.9.0/ddl/grants
--rollback revoke all on table water_statistics from dptb;

grant all on table water_statistics to dptb;

grant usage, select, update on sequence water_statistics_seq to dptb;