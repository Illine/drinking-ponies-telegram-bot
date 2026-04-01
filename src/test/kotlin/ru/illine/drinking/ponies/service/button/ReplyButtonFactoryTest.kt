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
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.model.base.WaterAmountType
import ru.illine.drinking.ponies.service.button.impl.ReplyButtonFactoryImpl
import ru.illine.drinking.ponies.service.button.strategy.snooze.SnoozeApplyReplyButtonStrategy
import ru.illine.drinking.ponies.service.button.strategy.wateramount.WaterAmountApplyReplyButtonStrategy
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.Clock

@UnitTest
@DisplayName("ReplyButtonFactory Unit Test")
class ReplyButtonFactoryTest {

    private lateinit var factory: ReplyButtonFactoryImpl

    @BeforeEach
    fun setUp() {
        val snoozeStrategy = SnoozeApplyReplyButtonStrategy(
            mock(TelegramClient::class.java),
            mock(NotificationAccessService::class.java),
            mock(WaterStatisticService::class.java),
            mock(MessageEditorService::class.java),
            Clock.systemUTC()
        )
        val waterAmountStrategy = WaterAmountApplyReplyButtonStrategy(
            mock(TelegramClient::class.java),
            mock(NotificationAccessService::class.java),
            mock(WaterStatisticService::class.java),
            mock(MessageEditorService::class.java),
            Clock.systemUTC()
        )
        factory = ReplyButtonFactoryImpl(listOf(snoozeStrategy, waterAmountStrategy))
    }

    @ParameterizedTest
    @EnumSource(SnoozeNotificationType::class)
    @DisplayName("getStrategy(): returns strategy for each SnoozeNotificationType queryData")
    fun `getStrategy returns strategy for snooze queryData`(snoozeType: SnoozeNotificationType) {
        val result = factory.getStrategy(snoozeType.queryData.toString())

        assertNotNull(result)
    }

    @ParameterizedTest
    @EnumSource(WaterAmountType::class)
    @DisplayName("getStrategy(): returns strategy for each WaterAmountType queryData")
    fun `getStrategy returns strategy for water amount queryData`(waterAmountType: WaterAmountType) {
        val result = factory.getStrategy(waterAmountType.queryData.toString())

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
