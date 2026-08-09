package ru.illine.drinking.ponies.bot

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.service.button.ReplyButtonFactory
import ru.illine.drinking.ponies.service.command.CommandService
import ru.illine.drinking.ponies.service.notification.NotificationService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.util.stream.Stream

@UnitTest
@DisplayName("DrinkingPoniesTelegramBot Unit Test")
class DrinkingPoniesTelegramBotTest {
    private lateinit var telegramUserAccessService: TelegramUserAccessService
    private lateinit var notificationService: NotificationService
    private lateinit var replyButtonFactory: ReplyButtonFactory
    private lateinit var bot: DrinkingPoniesTelegramBot

    @BeforeEach
    fun setUp() {
        telegramUserAccessService = mock<TelegramUserAccessService>()
        notificationService = mock<NotificationService>()
        replyButtonFactory = mock<ReplyButtonFactory>()
        bot =
            DrinkingPoniesTelegramBot(
                mock<TelegramClient>(),
                properties(),
                notificationService,
                replyButtonFactory,
                mock<CommandService>(),
                telegramUserAccessService,
            )
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideUpdatesCarryingAUser")
    @DisplayName("checkGlobalFlags(): a banned user is dropped before any ability or button runs")
    fun `checkGlobalFlags drops a banned user`(update: Update) {
        whenever(telegramUserAccessService.resolveAccessFlags(EXTERNAL_USER_ID))
            .thenReturn(DtoGenerator.generateUserAccessDto(externalUserId = EXTERNAL_USER_ID, isBanned = true))

        assertFalse(bot.checkGlobalFlags(update))

        verify(telegramUserAccessService).resolveAccessFlags(EXTERNAL_USER_ID)
        verifyNoInteractions(notificationService, replyButtonFactory)
    }

    @ParameterizedTest(name = "[{index}] isDeleted={0}")
    @CsvSource("false", "true")
    @DisplayName("checkGlobalFlags(): anyone who is not banned is let through, a soft-deleted user included")
    fun `checkGlobalFlags lets a user who is not banned through`(isDeleted: Boolean) {
        whenever(telegramUserAccessService.resolveAccessFlags(EXTERNAL_USER_ID))
            .thenReturn(DtoGenerator.generateUserAccessDto(externalUserId = EXTERNAL_USER_ID, isDeleted = isDeleted))

        assertTrue(bot.checkGlobalFlags(messageUpdate()), "A start has to be able to bring a deleted user back")

        verify(telegramUserAccessService).resolveAccessFlags(EXTERNAL_USER_ID)
    }

    @Test
    @DisplayName("checkGlobalFlags(): an update without a user is let through without asking for any flags")
    fun `checkGlobalFlags lets an update without a user through`() {
        assertTrue(bot.checkGlobalFlags(Update()))

        verifyNoInteractions(telegramUserAccessService, notificationService, replyButtonFactory)
    }

    private fun properties(): TelegramBotProperties =
        TelegramBotProperties(
            token = "token",
            username = "DrinkingPoniesBot",
            miniAppUrl = "https://example.test",
            autoUpdateTelegramConfig = false,
            http = TelegramBotProperties.Http(connectionTimeToLiveInSec = 60, maxConnectionTotal = 100),
        )

    companion object {
        private const val EXTERNAL_USER_ID = 777003L

        @JvmStatic
        fun provideUpdatesCarryingAUser(): Stream<Arguments> =
            Stream.of(
                Arguments.of(Named.of("a command", messageUpdate())),
                Arguments.of(Named.of("a button press", callbackUpdate())),
            )

        private fun messageUpdate(): Update =
            Update().apply {
                message =
                    Message().apply {
                        from = User(EXTERNAL_USER_ID, "Alisa", false)
                    }
            }

        private fun callbackUpdate(): Update =
            Update().apply {
                callbackQuery =
                    CallbackQuery().apply {
                        from = User(EXTERNAL_USER_ID, "Alisa", false)
                    }
            }
    }
}
