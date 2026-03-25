--liquibase formatted sql

--changeset illine:7.9.0/ddl/water_statistics

/* liquibase rollback
 drop table water_statistics;
 drop sequence water_statistics_seq;
*/

create sequence water_statistics_seq;

create table water_statistics
(
    id              bigint  default nextval('water_statistics_seq') not null
        primary key,
    user_id         bigint                                          not null
        references telegram_users (id),
    event_time      timestamp(0)                                    not null,
    event_type      text                                            not null,
    water_amount_ml integer default 0                               not null
);

comment on table water_statistics is 'Table storing water consumption statistics for users';
comment on column water_statistics.id is 'Primary key of the table';
comment on column water_statistics.user_id is 'Foreign key referencing the telegram users table';
comment on column water_statistics.event_time is 'Time when the event occurred, stored in UTC';
comment on column water_statistics.event_type is 'Type of the event: YES, SNOOZE, CANCEL';
comment on column water_statistics.water_amount_ml is 'Amount of water consumed in millilitres, 0 if not applicable';