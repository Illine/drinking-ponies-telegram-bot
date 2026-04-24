package ru.illine.drinking.ponies.service.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.*
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.service.notification.impl.NotificationSettingsServiceImpl
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime

@UnitTest
@DisplayName("NotificationSettingsService Unit Test")
class NotificationSettingsServiceTest {

    private val userId = 1L

    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var notificationTimeService: NotificationTimeService
    private lateinit var service: NotificationSettingsService

    @BeforeEach
    fun setUp() {
        notificationAccessService = mock(NotificationAccessService::class.java)
        notificationTimeService = mock(NotificationTimeService::class.java)
        service = NotificationSettingsServiceImpl(notificationAccessService, notificationTimeService)
    }

    @Test
    @DisplayName("changeQuietMode(): updates quiet mode via access service")
    fun `changeQuietMode updates quiet mode`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(8, 0)

        service.changeQuietMode(userId, start, end)

        verify(notificationAccessService).changeQuietMode(userId, start, end)
    }

    @Test
    @DisplayName("changeQuietMode(): throws IllegalArgumentException when start equals end")
    fun `changeQuietMode throws when start equals end`() {
        val time = LocalTime.of(10, 0)

        assertThrows(IllegalArgumentException::class.java) {
            service.changeQuietMode(userId, time, time)
        }
    }

    @Test
    @DisplayName("disableQuietMode(): disables quiet mode via access service")
    fun `disableQuietMode disables quiet mode`() {
        service.disableQuietMode(userId)

        verify(notificationAccessService).disableQuietMode(userId)
    }

    @Test
    @DisplayName("getNotificationSettings(): delegates to access service")
    fun `getNotificationSettings delegates to access service`() {
        val expected = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(expected)

        val result = service.getNotificationSettings(userId)

        assertEquals(expected, result)
        verify(notificationAccessService).findNotificationSettingByTelegramUserId(userId)
    }

    @Test
    @DisplayName("getAllNotificationSettings(): delegates to access service")
    fun `getAllNotificationSettings delegates to access service`() {
        val expected = setOf(DtoGenerator.generateNotificationDto())
        `when`(notificationAccessService.findAllNotificationSettings()).thenReturn(expected)

        val result = service.getAllNotificationSettings()

        assertEquals(expected, result)
        verify(notificationAccessService).findAllNotificationSettings()
    }

    @Test
    @DisplayName("resetNotificationTimer(): delegates to access service and returns DTO")
    fun `resetNotificationTimer delegates to access service`() {
        val time = LocalDateTime.of(2026, 4, 5, 12, 0)
        val expected = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.updateTimeOfLastNotification(userId, time)).thenReturn(expected)

        val result = service.resetNotificationTimer(userId, time)

        assertEquals(expected, result)
        verify(notificationAccessService).updateTimeOfLastNotification(userId, time)
    }

    @Test
    @DisplayName("isEnabledNotifications(): delegates to access service")
    fun `isEnabledNotifications delegates to access service`() {
        `when`(notificationAccessService.isEnabledNotifications(userId)).thenReturn(true)

        val result = service.isEnabledNotifications(userId)

        assertEquals(true, result)
        verify(notificationAccessService).isEnabledNotifications(userId)
    }

    @Test
    @DisplayName("isEnabledNotifications(): returns false when disabled")
    fun `isEnabledNotifications returns false when disabled`() {
        `when`(notificationAccessService.isEnabledNotifications(userId)).thenReturn(false)

        val result = service.isEnabledNotifications(userId)

        assertEquals(false, result)
        verify(notificationAccessService).isEnabledNotifications(userId)
    }

    @Test
    @DisplayName("changeInterval(): delegates to access service and returns DTO")
    fun `changeInterval delegates to access service`() {
        val interval = IntervalNotificationType.TWO_HOURS
        val expected = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.updateNotificationSettings(userId, interval)).thenReturn(expected)

        val result = service.changeInterval(userId, interval)

        assertEquals(expected, result)
        verify(notificationAccessService).updateNotificationSettings(userId, interval)
    }

    @Test
    @DisplayName("getQuietMode(): returns start and end times")
    fun `getQuietMode returns start and end`() {
        val expectedStart = LocalTime.of(23, 0)
        val expectedEnd = LocalTime.of(8, 0)
        val dto = DtoGenerator.generateNotificationDto(
            externalUserId = userId,
            quietModeStart = expectedStart,
            quietModeEnd = expectedEnd
        )
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(dto)

        val result = service.getQuietMode(userId)

        assertEquals(expectedStart, result.first)
        assertEquals(expectedEnd, result.second)
    }

    @Test
    @DisplayName("getQuietMode(): throws IllegalStateException when start is null")
    fun `getQuietMode throws when start is null`() {
        val dto = DtoGenerator.generateNotificationDto(
            externalUserId = userId,
            quietModeStart = null,
            quietModeEnd = LocalTime.of(8, 0)
        )
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(dto)

        assertThrows(IllegalStateException::class.java) {
            service.getQuietMode(userId)
        }
    }

    @Test
    @DisplayName("getQuietMode(): throws IllegalStateException when end is null")
    fun `getQuietMode throws when end is null`() {
        val dto = DtoGenerator.generateNotificationDto(
            externalUserId = userId,
            quietModeStart = LocalTime.of(23, 0),
            quietModeEnd = null
        )
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(dto)

        assertThrows(IllegalStateException::class.java) {
            service.getQuietMode(userId)
        }
    }

    @Test
    @DisplayName("changeNotificationStatus(): enables notifications when active is true")
    fun `changeNotificationStatus enables when active true`() {
        service.changeNotificationStatus(userId, true)

        verify(notificationAccessService).enableNotifications(userId)
        verify(notificationAccessService, never()).disableNotifications(anyLong())
    }

    @Test
    @DisplayName("changeNotificationStatus(): disables notifications when active is false")
    fun `changeNotificationStatus disables when active false`() {
        service.changeNotificationStatus(userId, false)

        verify(notificationAccessService).disableNotifications(userId)
        verify(notificationAccessService, never()).enableNotifications(anyLong())
    }

    @Test
    @DisplayName("changeTimezone(): updates timezone via access service")
    fun `changeTimezone updates timezone`() {
        val timezone = "America/New_York"

        service.changeTimezone(userId, timezone)

        verify(notificationAccessService).changeTimezone(userId, timezone)
    }

    @ParameterizedTest
    @ValueSource(strings = ["Invalid", "ABC/XYZ", "123", ""])
    @DisplayName("changeTimezone(): throws IllegalArgumentException for invalid timezone")
    fun `changeTimezone throws when invalid timezone`(timezone: String) {
        assertThrows(IllegalArgumentException::class.java) {
            service.changeTimezone(userId, timezone)
        }

        verify(notificationAccessService, never()).changeTimezone(anyLong(), anyString())
    }

    @Test
    @DisplayName("getNextNotificationAt(): delegates to time service and returns instant")
    fun `getNextNotificationAt delegates to time service`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        val expectedInstant = Instant.parse("2025-01-01T11:00:00Z")
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(dto)
        `when`(notificationTimeService.calculateNextNotificationAt(dto)).thenReturn(expectedInstant)

        val result = service.getNextNotificationAt(userId)

        assertEquals(expectedInstant, result)
        verify(notificationAccessService).findNotificationSettingByTelegramUserId(userId)
        verify(notificationTimeService).calculateNextNotificationAt(dto)
    }

    @Test
    @DisplayName("getAllSettings(): returns SettingDto with all fields when notifications are enabled")
    fun `getAllSettings returns full dto when enabled`() {
        `when`(notificationAccessService.isEnabledNotifications(userId)).thenReturn(true)
        val notificationDto = DtoGenerator.generateNotificationDto(
            externalUserId = userId,
            notificationInterval = IntervalNotificationType.HOUR,
            userTimeZone = "Europe/Moscow",
            quietModeStart = LocalTime.of(23, 0),
            quietModeEnd = LocalTime.of(8, 0),
        )
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        val result = service.getAllSettings(userId)

        assertEquals(true, result.notificationActive)
        assertEquals(IntervalNotificationType.HOUR.name, result.interval)
        assertEquals(IntervalNotificationType.HOUR.displayName, result.intervalDisplayName)
        assertEquals(IntervalNotificationType.HOUR.minutes, result.intervalMinutes)
        assertEquals("23:00", result.quietModeStart)
        assertEquals("08:00", result.quietModeEnd)
        assertEquals("Europe/Moscow", result.timezone)
        verify(notificationAccessService).isEnabledNotifications(userId)
        verify(notificationAccessService).findNotificationSettingByTelegramUserId(userId)
    }

    @Test
    @DisplayName("getAllSettings(): returns SettingDto with only notificationActive=false when notifications are disabled")
    fun `getAllSettings returns minimal dto when disabled`() {
        `when`(notificationAccessService.isEnabledNotifications(userId)).thenReturn(false)

        val result = service.getAllSettings(userId)

        assertEquals(false, result.notificationActive)
        assertEquals(null, result.interval)
        assertEquals(null, result.intervalDisplayName)
        assertEquals(null, result.intervalMinutes)
        assertEquals(null, result.quietModeStart)
        assertEquals(null, result.quietModeEnd)
        assertEquals(null, result.timezone)
        verify(notificationAccessService).isEnabledNotifications(userId)
        verify(notificationAccessService, never()).findNotificationSettingByTelegramUserId(anyLong())
    }
}
