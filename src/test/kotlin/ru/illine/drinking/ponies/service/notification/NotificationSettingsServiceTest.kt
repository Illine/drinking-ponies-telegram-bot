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
import java.time.*

@UnitTest
@DisplayName("NotificationSettingsService Unit Test")
class NotificationSettingsServiceTest {

    private val externalUserId = 1L

    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var notificationTimeService: NotificationTimeService
    private lateinit var clock: Clock
    private lateinit var service: NotificationSettingsService

    @BeforeEach
    fun setUp() {
        notificationAccessService = mock(NotificationAccessService::class.java)
        notificationTimeService = mock(NotificationTimeService::class.java)
        clock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC)
        service = NotificationSettingsServiceImpl(notificationAccessService, notificationTimeService, clock)
    }

    @Test
    @DisplayName("changeQuietMode(): updates quiet mode via access service")
    fun `changeQuietMode updates quiet mode`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(8, 0)

        service.changeQuietMode(externalUserId, start, end)

        verify(notificationAccessService).changeQuietMode(externalUserId, start, end)
    }

    @Test
    @DisplayName("changeQuietMode(): throws IllegalArgumentException when start equals end")
    fun `changeQuietMode throws when start equals end`() {
        val time = LocalTime.of(10, 0)

        assertThrows(IllegalArgumentException::class.java) {
            service.changeQuietMode(externalUserId, time, time)
        }
    }

    @Test
    @DisplayName("disableQuietMode(): disables quiet mode via access service")
    fun `disableQuietMode disables quiet mode`() {
        service.disableQuietMode(externalUserId)

        verify(notificationAccessService).disableQuietMode(externalUserId)
    }

    @Test
    @DisplayName("getNotificationSettings(): delegates to access service")
    fun `getNotificationSettings delegates to access service`() {
        val expected = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        `when`(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)).thenReturn(expected)

        val result = service.getNotificationSettings(externalUserId)

        assertEquals(expected, result)
        verify(notificationAccessService).findNotificationSettingByExternalUserId(externalUserId)
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
        val expected = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        `when`(notificationAccessService.updateTimeOfLastNotification(externalUserId, time)).thenReturn(expected)

        val result = service.resetNotificationTimer(externalUserId, time)

        assertEquals(expected, result)
        verify(notificationAccessService).updateTimeOfLastNotification(externalUserId, time)
    }

    @Test
    @DisplayName("isEnabledNotifications(): delegates to access service")
    fun `isEnabledNotifications delegates to access service`() {
        `when`(notificationAccessService.isEnabledNotifications(externalUserId)).thenReturn(true)

        val result = service.isEnabledNotifications(externalUserId)

        assertEquals(true, result)
        verify(notificationAccessService).isEnabledNotifications(externalUserId)
    }

    @Test
    @DisplayName("isEnabledNotifications(): returns false when disabled")
    fun `isEnabledNotifications returns false when disabled`() {
        `when`(notificationAccessService.isEnabledNotifications(externalUserId)).thenReturn(false)

        val result = service.isEnabledNotifications(externalUserId)

        assertEquals(false, result)
        verify(notificationAccessService).isEnabledNotifications(externalUserId)
    }

    @Test
    @DisplayName("changeInterval(): delegates to access service and returns DTO")
    fun `changeInterval delegates to access service`() {
        val interval = IntervalNotificationType.TWO_HOURS
        val expected = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        `when`(notificationAccessService.updateNotificationSettings(externalUserId, interval)).thenReturn(expected)

        val result = service.changeInterval(externalUserId, interval)

        assertEquals(expected, result)
        verify(notificationAccessService).updateNotificationSettings(externalUserId, interval)
    }

    @Test
    @DisplayName("getQuietMode(): returns start and end times")
    fun `getQuietMode returns start and end`() {
        val expectedStart = LocalTime.of(23, 0)
        val expectedEnd = LocalTime.of(8, 0)
        val dto = DtoGenerator.generateNotificationDto(
            externalUserId = externalUserId,
            quietModeStart = expectedStart,
            quietModeEnd = expectedEnd
        )
        `when`(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)).thenReturn(dto)

        val result = service.getQuietMode(externalUserId)

        assertEquals(expectedStart, result.first)
        assertEquals(expectedEnd, result.second)
    }

    @Test
    @DisplayName("getQuietMode(): throws IllegalStateException when start is null")
    fun `getQuietMode throws when start is null`() {
        val dto = DtoGenerator.generateNotificationDto(
            externalUserId = externalUserId,
            quietModeStart = null,
            quietModeEnd = LocalTime.of(8, 0)
        )
        `when`(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)).thenReturn(dto)

        assertThrows(IllegalStateException::class.java) {
            service.getQuietMode(externalUserId)
        }
    }

    @Test
    @DisplayName("getQuietMode(): throws IllegalStateException when end is null")
    fun `getQuietMode throws when end is null`() {
        val dto = DtoGenerator.generateNotificationDto(
            externalUserId = externalUserId,
            quietModeStart = LocalTime.of(23, 0),
            quietModeEnd = null
        )
        `when`(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)).thenReturn(dto)

        assertThrows(IllegalStateException::class.java) {
            service.getQuietMode(externalUserId)
        }
    }

    @Test
    @DisplayName("changeNotificationStatus(): enables notifications when active is true")
    fun `changeNotificationStatus enables when active true`() {
        service.changeNotificationStatus(externalUserId, true)

        verify(notificationAccessService).enableNotifications(externalUserId)
        verify(notificationAccessService, never()).disableNotifications(anyLong())
    }

    @Test
    @DisplayName("changeNotificationStatus(): disables notifications when active is false")
    fun `changeNotificationStatus disables when active false`() {
        service.changeNotificationStatus(externalUserId, false)

        verify(notificationAccessService).disableNotifications(externalUserId)
        verify(notificationAccessService, never()).enableNotifications(anyLong())
    }

    @Test
    @DisplayName("changeTimezone(): updates timezone via access service")
    fun `changeTimezone updates timezone`() {
        val timezone = "America/New_York"

        service.changeTimezone(externalUserId, timezone)

        verify(notificationAccessService).changeTimezone(externalUserId, timezone)
    }

    @ParameterizedTest
    @ValueSource(strings = ["Invalid", "ABC/XYZ", "123", ""])
    @DisplayName("changeTimezone(): throws IllegalArgumentException for invalid timezone")
    fun `changeTimezone throws when invalid timezone`(timezone: String) {
        assertThrows(IllegalArgumentException::class.java) {
            service.changeTimezone(externalUserId, timezone)
        }

        verify(notificationAccessService, never()).changeTimezone(anyLong(), anyString())
    }

    @Test
    @DisplayName("getNextNotificationAt(): delegates to time service and returns instant")
    fun `getNextNotificationAt delegates to time service`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        val expectedInstant = Instant.parse("2025-01-01T11:00:00Z")
        `when`(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)).thenReturn(dto)
        `when`(notificationTimeService.calculateNextNotificationAt(dto)).thenReturn(expectedInstant)

        val result = service.getNextNotificationAt(externalUserId)

        assertEquals(expectedInstant, result)
        verify(notificationAccessService).findNotificationSettingByExternalUserId(externalUserId)
        verify(notificationTimeService).calculateNextNotificationAt(dto)
    }

    @Test
    @DisplayName("getAllSettings(): returns SettingDto with all fields when notifications are enabled")
    fun `getAllSettings returns full dto when enabled`() {
        `when`(notificationAccessService.isEnabledNotifications(externalUserId)).thenReturn(true)
        val notificationDto = DtoGenerator.generateNotificationDto(
            externalUserId = externalUserId,
            notificationInterval = IntervalNotificationType.HOUR,
            userTimeZone = "Europe/Moscow",
            quietModeStart = LocalTime.of(23, 0),
            quietModeEnd = LocalTime.of(8, 0),
        )
        `when`(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)).thenReturn(notificationDto)

        val result = service.getAllSettings(externalUserId)

        assertEquals(true, result.notificationActive)
        assertEquals(IntervalNotificationType.HOUR.name, result.interval)
        assertEquals(IntervalNotificationType.HOUR.displayName, result.intervalDisplayName)
        assertEquals(IntervalNotificationType.HOUR.minutes, result.intervalMinutes)
        assertEquals("23:00", result.quietModeStart)
        assertEquals("08:00", result.quietModeEnd)
        assertEquals("Europe/Moscow", result.timezone)
        assertEquals(2000, result.dailyGoalMl)
        verify(notificationAccessService).isEnabledNotifications(externalUserId)
        verify(notificationAccessService).findNotificationSettingByExternalUserId(externalUserId)
    }

    @Test
    @DisplayName("getAllSettings(): returns SettingDto with only notificationActive=false when notifications are disabled")
    fun `getAllSettings returns minimal dto when disabled`() {
        `when`(notificationAccessService.isEnabledNotifications(externalUserId)).thenReturn(false)

        val result = service.getAllSettings(externalUserId)

        assertEquals(false, result.notificationActive)
        assertEquals(null, result.interval)
        assertEquals(null, result.intervalDisplayName)
        assertEquals(null, result.intervalMinutes)
        assertEquals(null, result.quietModeStart)
        assertEquals(null, result.quietModeEnd)
        assertEquals(null, result.timezone)
        assertEquals(null, result.dailyGoalMl)
        verify(notificationAccessService).isEnabledNotifications(externalUserId)
        verify(notificationAccessService, never()).findNotificationSettingByExternalUserId(anyLong())
    }

    @Test
    @DisplayName("pauseNotifications(): computes pauseUntil as now(UTC) + minutes and delegates to access service")
    fun `pauseNotifications computes pauseUntil and delegates`() {
        val minutes = 240L
        val expectedPauseUntil = LocalDateTime.of(2025, 1, 1, 16, 0)

        service.pauseNotifications(externalUserId, minutes)

        verify(notificationAccessService).setPause(externalUserId, expectedPauseUntil)
    }

    @ParameterizedTest
    @ValueSource(longs = [0, -1, -100])
    @DisplayName("pauseNotifications(): throws IllegalArgumentException when minutes is not positive")
    fun `pauseNotifications throws when minutes not positive`(minutes: Long) {
        assertThrows(IllegalArgumentException::class.java) {
            service.pauseNotifications(externalUserId, minutes)
        }

        verify(notificationAccessService, never()).setPause(anyLong(), any())
    }

    @Test
    @DisplayName("cancelPause(): delegates to access service with null pauseUntil")
    fun `cancelPause delegates with null`() {
        service.cancelPause(externalUserId)

        verify(notificationAccessService).setPause(externalUserId, null)
    }

    @Test
    @DisplayName("getPauseState(): returns paused=true and ISO UTC pauseUntil when pause is active")
    fun `getPauseState returns paused true when active`() {
        val pauseUntil = LocalDateTime.of(2025, 1, 1, 13, 0)
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, pauseUntil = pauseUntil)
        `when`(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)).thenReturn(dto)

        val result = service.getPauseState(externalUserId)

        assertEquals(true, result.paused)
        assertEquals(Instant.parse("2025-01-01T13:00:00Z"), result.pauseUntil)
    }

    @Test
    @DisplayName("getPauseState(): returns paused=false and null pauseUntil when pauseUntil is in the past")
    fun `getPauseState returns paused false when pauseUntil expired`() {
        val pauseUntil = LocalDateTime.of(2025, 1, 1, 11, 0)
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, pauseUntil = pauseUntil)
        `when`(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)).thenReturn(dto)

        val result = service.getPauseState(externalUserId)

        assertEquals(false, result.paused)
        assertEquals(null, result.pauseUntil)
    }

    @Test
    @DisplayName("getPauseState(): returns paused=false and null pauseUntil when pauseUntil is null")
    fun `getPauseState returns paused false when pauseUntil null`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, pauseUntil = null)
        `when`(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)).thenReturn(dto)

        val result = service.getPauseState(externalUserId)

        assertEquals(false, result.paused)
        assertEquals(null, result.pauseUntil)
    }

    @Test
    @DisplayName("getPauseState(): returns paused=false when pauseUntil equals now")
    fun `getPauseState returns paused false when pauseUntil equals now`() {
        val pauseUntil = LocalDateTime.of(2025, 1, 1, 12, 0)
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, pauseUntil = pauseUntil)
        `when`(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)).thenReturn(dto)

        val result = service.getPauseState(externalUserId)

        assertEquals(false, result.paused)
        assertEquals(null, result.pauseUntil)
    }

    @Test
    @DisplayName("pauseNotifications(): re-pause overrides existing pauseUntil with newly computed value")
    fun `pauseNotifications re-pause delegates with new pauseUntil`() {
        val firstMinutes = 60L
        val secondMinutes = 240L
        val firstExpected = LocalDateTime.of(2025, 1, 1, 13, 0)
        val secondExpected = LocalDateTime.of(2025, 1, 1, 16, 0)

        service.pauseNotifications(externalUserId, firstMinutes)
        service.pauseNotifications(externalUserId, secondMinutes)

        verify(notificationAccessService).setPause(externalUserId, firstExpected)
        verify(notificationAccessService).setPause(externalUserId, secondExpected)
    }

    @Test
    @DisplayName("pauseNotifications(): does not call cancelPause path")
    fun `pauseNotifications does not cancel`() {
        service.pauseNotifications(externalUserId, 30L)

        verify(notificationAccessService, never()).setPause(externalUserId, null)
    }

    @ParameterizedTest
    @ValueSource(ints = [2000, 2250, 2500, 2750, 3000])
    @DisplayName("changeDailyGoal(): delegates to access service for any allowed value (incl. 2000 and 3000 boundaries)")
    fun `changeDailyGoal delegates for allowed value`(goalMl: Int) {
        service.changeDailyGoal(externalUserId, goalMl)

        verify(notificationAccessService).updateDailyGoal(externalUserId, goalMl)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 99, 1500, 1999, 2001, 2100, 2999, 3001, 10001, -100])
    @DisplayName("changeDailyGoal(): throws IllegalArgumentException for value outside ALLOWED_VALUES_ML")
    fun `changeDailyGoal throws when value not allowed`(goalMl: Int) {
        assertThrows(IllegalArgumentException::class.java) {
            service.changeDailyGoal(externalUserId, goalMl)
        }

        verify(notificationAccessService, never()).updateDailyGoal(anyLong(), anyInt())
    }

    @Test
    @DisplayName("getPauseState(): returns paused=true even when notificationAttempts > 0 (state independent)")
    fun `getPauseState returns paused true regardless of notificationAttempts`() {
        val pauseUntil = LocalDateTime.of(2025, 1, 1, 13, 0)
        val dto = DtoGenerator.generateNotificationDto(
            externalUserId = externalUserId,
            pauseUntil = pauseUntil,
            notificationAttempts = 3
        )
        `when`(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)).thenReturn(dto)

        val result = service.getPauseState(externalUserId)

        assertEquals(true, result.paused)
        assertEquals(Instant.parse("2025-01-01T13:00:00Z"), result.pauseUntil)
    }
}
