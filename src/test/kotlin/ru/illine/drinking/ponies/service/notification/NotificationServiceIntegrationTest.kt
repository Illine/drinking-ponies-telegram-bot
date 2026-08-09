package ru.illine.drinking.ponies.service.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.config.cache.CacheConfig
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.model.dto.internal.UserStateChangeDto
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import java.time.Clock
import java.time.LocalDateTime

@SpringIntegrationTest
@DisplayName("NotificationService Spring Integration Test")
@Sql(
    scripts = ["classpath:sql/clear.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class NotificationServiceIntegrationTest
    @Autowired
    constructor(
        private val notificationService: NotificationService,
        private val notificationAccessService: NotificationAccessService,
        private val telegramUserAccessService: TelegramUserAccessService,
        private val cacheManager: CacheManager,
        private val jdbcTemplate: JdbcTemplate,
        private val clock: Clock,
    ) {
        @MockitoBean
        private lateinit var sender: TelegramClient

        @BeforeEach
        fun clearCache() {
            cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)?.clear()
        }

        @Test
        @DisplayName("start(): a soft-deleted user says /start again - the account comes back instead of blowing up")
        fun `start brings a soft deleted user back`() {
            notificationService.start(messageContext())
            softDelete()

            assertDoesNotThrow { notificationService.start(messageContext()) }

            assertFalse(isDeleted(), "The account has to be usable again after /start")
        }

        @Test
        @DisplayName("start(): the returning user keeps a single account, chat and settings row")
        fun `start does not duplicate anything`() {
            notificationService.start(messageContext())
            softDelete()

            notificationService.start(messageContext())

            assertEquals(1, countUsers(), "A second telegram_users row would break the unique index")
            assertEquals(1, countChats())
            assertEquals(1, countSettings())
        }

        @Test
        @DisplayName("start(): drops the cached access flags, so the stale isDeleted is not served on")
        fun `start evicts the cached access flags`() {
            notificationService.start(messageContext())
            softDelete()
            val cache = cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)!!
            telegramUserAccessService.resolveAccessFlags(EXTERNAL_USER_ID)
            assertNotNull(cache.get(EXTERNAL_USER_ID))

            notificationService.start(messageContext())

            assertNull(cache.get(EXTERNAL_USER_ID))
        }

        @Test
        @DisplayName("start(): the restored user is back in the mailing selection")
        fun `start puts the user back into the mailing`() {
            notificationService.start(messageContext())
            softDelete()
            assertTrue(mailingRecipients().isEmpty(), "A deleted user is not reminded to drink")

            notificationService.start(messageContext())

            assertEquals(listOf(EXTERNAL_USER_ID), mailingRecipients())
        }

        @Test
        @DisplayName("start(): a live user is untouched - no restore, no second account")
        fun `start leaves a live user alone`() {
            notificationService.start(messageContext())

            notificationService.start(messageContext())

            assertFalse(isDeleted())
            assertEquals(1, countUsers())
            assertEquals(1, countSettings())
        }

        @Test
        @DisplayName("start(): a first-time user gets an account, a chat and settings")
        fun `start registers a newcomer`() {
            notificationService.start(messageContext())

            assertEquals(1, countUsers())
            assertEquals(1, countChats())
            assertEquals(1, countSettings())
            assertFalse(isDeleted())
        }

        @Test
        @DisplayName("start(): a user who switched notifications off says /start - it works and turns them back on")
        fun `start revives a user with notifications switched off`() {
            notificationService.start(messageContext())
            notificationAccessService.updateNotificationsDisabled(EXTERNAL_USER_ID)

            assertDoesNotThrow { notificationService.start(messageContext()) }

            assertTrue(notificationAccessService.findIsEnabledNotificationsByExternalUserId(EXTERNAL_USER_ID))
        }

        @Test
        @DisplayName("start(): an active pause does not outlive the /start that turns notifications back on")
        fun `start clears an active pause`() {
            notificationService.start(messageContext())
            val now = LocalDateTime.now(clock)
            notificationAccessService.updatePause(EXTERNAL_USER_ID, now.plusHours(8))

            notificationService.start(messageContext())

            val settings = notificationAccessService.findNotificationSettingByExternalUserId(EXTERNAL_USER_ID)
            assertNull(settings.pauseUntil)
            assertFalse(
                settings.timeOfLastNotification.isAfter(now),
                "the bot answers that notifications are on, so they have to actually arrive",
            )
        }

        @Test
        @DisplayName("start(): the ban survives a /start, unlike a deletion")
        fun `start does not lift a ban`() {
            notificationService.start(messageContext())
            ban()

            notificationService.start(messageContext())

            assertTrue(telegramUserAccessService.resolveAccessFlags(EXTERNAL_USER_ID).isBanned)
        }

        @Test
        @DisplayName("a banned user is dropped from the mailing selection")
        fun `a banned user is not reminded to drink`() {
            notificationService.start(messageContext())
            assertEquals(listOf(EXTERNAL_USER_ID), mailingRecipients())

            ban()

            assertTrue(mailingRecipients().isEmpty(), "A banned user gets no notifications")
        }

        @Test
        @DisplayName("start(): the restore is recorded with the returning user as their own actor")
        fun `start records the restore as the deed of the user themselves`() {
            notificationService.start(messageContext())
            softDelete()

            notificationService.start(messageContext())

            val id = userId()
            assertEquals(listOf("RESTORED by $id on $id"), stateEvents().filter { it.startsWith("RESTORED") })
        }

        @Test
        @DisplayName("start(): a live user says /start - the audit history stays empty, nothing changed")
        fun `start records nothing for a live user`() {
            notificationService.start(messageContext())

            notificationService.start(messageContext())

            assertTrue(stateEvents().isEmpty(), "An unchanged state is not a state event")
        }

        private fun mailingRecipients(): List<Long> =
            notificationAccessService.findAllNotificationSettings().map { it.telegramUser.externalUserId }

        private fun stateEvents(): List<String> =
            jdbcTemplate.query(SELECT_EVENTS) { rs, _ ->
                "${rs.getString("event_type")} by ${rs.getLong("actor_user_id")} on ${rs.getLong("user_id")}"
            }

        private fun userId(): Long = jdbcTemplate.queryForObject(SELECT_ID, Long::class.java, EXTERNAL_USER_ID)!!

        // The admin API refuses to change one's own state, so an arrange step doing it describes a world
        // that cannot happen - both fixtures act as somebody else.
        private fun adminId(): Long = jdbcTemplate.queryForObject(UPSERT_ADMIN, Long::class.java, ADMIN_EXTERNAL_ID)!!

        private fun softDelete() {
            telegramUserAccessService.updateState(userId(), adminId(), UserStateChangeDto(deleted = true))
            assertTrue(isDeleted(), "Arrange step must really have soft deleted the user")
        }

        private fun ban() {
            telegramUserAccessService.updateState(userId(), adminId(), UserStateChangeDto(banned = true))
        }

        private fun isDeleted(): Boolean =
            jdbcTemplate.queryForObject(SELECT_DELETED, Boolean::class.java, EXTERNAL_USER_ID)!!

        private fun countUsers(): Int = jdbcTemplate.queryForObject(COUNT_USERS, Int::class.java, EXTERNAL_USER_ID)!!

        private fun countChats(): Int = jdbcTemplate.queryForObject(COUNT_CHATS, Int::class.java, EXTERNAL_USER_ID)!!

        private fun countSettings(): Int =
            jdbcTemplate.queryForObject(COUNT_SETTINGS, Int::class.java, EXTERNAL_USER_ID)!!

        private fun messageContext(): MessageContext {
            val user = mock<User>()
            whenever(user.id).thenReturn(EXTERNAL_USER_ID)
            whenever(user.userName).thenReturn("carolgone")
            whenever(user.firstName).thenReturn("Carol")
            whenever(user.lastName).thenReturn("Ivanova")

            val context = mock<MessageContext>()
            whenever(context.user()).thenReturn(user)
            whenever(context.chatId()).thenReturn(CHAT_ID)
            return context
        }

        companion object {
            private const val EXTERNAL_USER_ID = 777001L
            private const val ADMIN_EXTERNAL_ID = 777099L
            private const val CHAT_ID = 424242L

            private const val UPSERT_ADMIN = """
                insert into telegram_users (external_user_id, user_time_zone, is_admin)
                values (?, 'Europe/Moscow', true)
                on conflict (external_user_id) do update set is_admin = true
                returning id
            """

            private const val SELECT_ID = "select id from telegram_users where external_user_id = ?"
            private const val SELECT_EVENTS =
                "select event_type, user_id, actor_user_id from user_state_events order by id"
            private const val SELECT_DELETED = "select deleted from telegram_users where external_user_id = ?"
            private const val COUNT_USERS = "select count(*) from telegram_users where external_user_id = ?"
            private const val COUNT_CHATS = """
                select count(*)
                from telegram_chats c
                         join telegram_users u on u.id = c.telegram_user_id
                where u.external_user_id = ?
            """
            private const val COUNT_SETTINGS = """
                select count(*)
                from notification_settings s
                         join telegram_users u on u.id = s.telegram_user_id
                where u.external_user_id = ?
            """
        }
    }
