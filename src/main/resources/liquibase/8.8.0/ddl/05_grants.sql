--liquibase formatted sql

--changeset illine:8.8.0/ddl/grants
--rollback revoke all on table user_state_events from dptb;

grant all on table user_state_events to dptb;

grant usage, select, update on sequence user_state_event_seq to dptb;
