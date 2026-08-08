package ru.illine.drinking.ponies.dao.repository.query

object TelegramUserQuery {
    const val ADMIN_USER_SELECT = """
        select u.id               as "id",
               u.external_user_id as "externalUserId",
               u.first_name       as "firstName",
               u.last_name        as "lastName",
               u.username         as "username",
               u.is_admin         as "admin",
               u.is_banned        as "banned",
               u.deleted          as "deleted",
               u.user_time_zone   as "timeZone",
               u.created          as "created",
               (
                   select max(ws.event_time)
                   from water_statistics ws
                   where ws.user_id = u.id
                     and ws.event_type in ('YES', 'SNOOZE')
               )                  as "lastActivity"
        from telegram_users u
    """

    const val SEARCH_FILTER = """
        (
            cast(:search as text) is null
            or concat_ws(' ', u.first_name, u.last_name, u.username, u.external_user_id, u.id)
                   ilike cast(:search as text) escape '\'
        )
    """
}
