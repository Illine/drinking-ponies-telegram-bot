-- Users are inserted without an explicit id: clear.sql restarts the sequence, so the internal ids
-- are 1..8 in insert order. From the third user on, the Telegram id is deliberately far away from
-- the internal one, so a query that confuses the two cannot stay unnoticed.
insert into telegram_users (external_user_id, user_time_zone, is_admin, is_banned,
                            first_name, last_name, username, created, deleted)
values
    -- id = 1, admin, active
    (1, 'Europe/Moscow', true, false,
     'Alisa', 'Petrova', 'alisaadmin', timestamp '2026-01-01 10:00:00', false),
    -- id = 2, plain active user
    (2, 'Europe/Moscow', false, false,
     'Bob', 'Smith', 'bobsmith', timestamp '2026-01-02 11:00:00', false),
    -- id = 3, soft deleted - has to stay visible for the admin endpoints
    (777001, 'Asia/Kolkata', false, false,
     'Carol', 'Ivanova', 'carolgone', timestamp '2026-01-03 12:00:00', true),
    -- id = 4, banned
    (777002, 'Europe/Moscow', false, true,
     'Dave', 'Jones', 'davebanned', timestamp '2026-01-04 13:00:00', false),
    -- id = 5, active, never answered a notification
    (777003, 'Europe/Moscow', false, false,
     'Eve', 'Watson', 'evefresh', timestamp '2026-01-05 14:00:00', false),
    -- id = 6, banned AND soft deleted: the overlap that makes the mutual exclusion of the
    -- counters a real assertion instead of one that holds because the data avoids the case.
    -- A ban wins, so this one belongs to BANNED and must not appear under INACTIVE.
    (777004, 'Europe/Moscow', false, true,
     'Frank', 'Miller', 'frankgone', timestamp '2026-01-06 15:00:00', true),
    -- id = 7 and id = 8, a pair that only an escaped LIKE can tell apart: "_" is a single-character
    -- wildcard, so an unescaped search for alice_p would match aliceXp as well.
    (888001, 'Europe/Moscow', false, false,
     'Alice', 'Pike', 'alice_p', timestamp '2026-01-07 16:00:00', false),
    (888002, 'Europe/Moscow', false, false,
     'Alice', 'Xavier', 'aliceXp', timestamp '2026-01-08 17:00:00', false);

-- Last activity is the newest YES or SNOOZE. The CANCEL of user 4 must not count, and users 4 to 8
-- therefore have no last activity at all - they are the ones expected at the tail of the list.
insert into water_statistics (user_id, event_time, event_type, water_amount_ml)
values (1, timestamp '2026-03-04 09:00:00', 'SNOOZE', 0),
       (2, timestamp '2026-03-01 10:00:00', 'YES', 250),
       (2, timestamp '2026-03-05 08:00:00', 'YES', 300),
       (3, timestamp '2026-03-06 12:00:00', 'YES', 200),
       (4, timestamp '2026-03-07 07:00:00', 'CANCEL', 0);
