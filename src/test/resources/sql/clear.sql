delete from notifications
where id is not null;

alter sequence notification_seq restart with 1;