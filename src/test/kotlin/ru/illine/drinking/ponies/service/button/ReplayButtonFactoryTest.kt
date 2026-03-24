package ru.illine.drinking.ponies.service.button

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.service.button.impl.ReplayButtonFactoryImpl
import ru.illine.drinking.ponies.service.button.strategy.snooze.SnoozeApplyReplayButtonStrategy
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.Clock

@UnitTest
@DisplayName("ReplayButtonFactory Unit Test")
class ReplayButtonFactoryTest {

    private lateinit var factory: ReplayButtonFactoryImpl

    @BeforeEach
    fun setUp() {
        val strategy = SnoozeApplyReplayButtonStrategy(
            mock(TelegramClient::class.java),
            mock(NotificationAccessService::class.java),
            mock(MessageEditorService::class.java),
            Clock.systemUTC()
        )
        factory = ReplayButtonFactoryImpl(listOf(strategy))
    }

    @ParameterizedTest
    @EnumSource(SnoozeNotificationType::class)
    @DisplayName("getStrategy(): returns strategy for each SnoozeNotificationType queryData")
    fun `getStrategy returns strategy for snooze queryData`(snoozeType: SnoozeNotificationType) {
        val result = factory.getStrategy(snoozeType.queryData.toString())

        assertNotNull(result)
    }

    @Test
    @DisplayName("getStrategy(): throws when no strategy matches queryData")
    fun `getStrategy throws when no strategy matches`() {
        assertThrows(IllegalArgumentException::class.java) {
            factory.getStrategy("00000000-0000-0000-0000-000000000000")
        }
    }
}
