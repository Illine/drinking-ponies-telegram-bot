delete from drinking_ponies.notifications
where id is not null;

alter sequence drinking_ponies.notification_seq restart with 1;