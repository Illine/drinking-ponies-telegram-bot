package ru.illine.drinking.ponies.scheduler

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.illine.drinking.ponies.service.notification.NotificationSenderService
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.service.notification.NotificationTimeService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("NotificationScheduler Unit Test")
class NotificationSchedulerTest {

    private lateinit var notificationSettingsService: NotificationSettingsService
    private lateinit var notificationSenderService: NotificationSenderService
    private lateinit var notificationTimeService: NotificationTimeService
    private lateinit var scheduler: NotificationScheduler

    @BeforeEach
    fun setUp() {
        notificationSettingsService = mock<NotificationSettingsService>()
        notificationSenderService = mock<NotificationSenderService>()
        notificationTimeService = mock<NotificationTimeService>()
        scheduler = NotificationScheduler(notificationSettingsService, notificationSenderService, notificationTimeService)
    }

    @Test
    @DisplayName("sendDrinkingReminders(): no interactions when no notifications exist")
    fun `sendDrinkingReminders does nothing when no notifications`() {
        whenever(notificationSettingsService.getAllNotificationSettings()).thenReturn(emptySet())

        scheduler.sendDrinkingReminders()

        verify(notificationSenderService, never()).sendNotifications(any())
        verify(notificationSenderService, never()).suspendNotifications(any())
    }

    @Test
    @DisplayName("sendDrinkingReminders(): skips disabled notifications")
    fun `sendDrinkingReminders filters out disabled notifications`() {
        val disabled = DtoGenerator.generateNotificationDto().copy(enabled = false)
        whenever(notificationSettingsService.getAllNotificationSettings()).thenReturn(setOf(disabled))

        scheduler.sendDrinkingReminders()

        verify(notificationSenderService, never()).sendNotifications(any())
        verify(notificationSenderService, never()).suspendNotifications(any())
    }

    @Test
    @DisplayName("sendDrinkingReminders(): skips notifications inside quiet time")
    fun `sendDrinkingReminders filters out quiet time notifications`() {
        val dto = DtoGenerator.generateNotificationDto()
        whenever(notificationSettingsService.getAllNotificationSettings()).thenReturn(setOf(dto))
        doReturn(false).whenever(notificationTimeService).isOutsideQuietTime(dto)

        scheduler.sendDrinkingReminders()

        verify(notificationSenderService, never()).sendNotifications(any())
        verify(notificationSenderService, never()).suspendNotifications(any())
    }

    @Test
    @DisplayName("sendDrinkingReminders(): skips notifications not yet due")
    fun `sendDrinkingReminders filters out not due notifications`() {
        val dto = DtoGenerator.generateNotificationDto()
        whenever(notificationSettingsService.getAllNotificationSettings()).thenReturn(setOf(dto))
        doReturn(true).whenever(notificationTimeService).isOutsideQuietTime(dto)
        doReturn(false).whenever(notificationTimeService).isNotificationDue(dto)

        scheduler.sendDrinkingReminders()

        verify(notificationSenderService, never()).sendNotifications(any())
        verify(notificationSenderService, never()).suspendNotifications(any())
    }

    @Test
    @DisplayName("sendDrinkingReminders(): sends active notification (attempts < 3)")
    fun `sendDrinkingReminders sends active notification`() {
        val dto = DtoGenerator.generateNotificationDto(notificationAttempts = 0)
        whenever(notificationSettingsService.getAllNotificationSettings()).thenReturn(setOf(dto))
        doReturn(true).whenever(notificationTimeService).isOutsideQuietTime(dto)
        doReturn(true).whenever(notificationTimeService).isNotificationDue(dto)

        scheduler.sendDrinkingReminders()

        verify(notificationSenderService).sendNotifications(listOf(dto))
        verify(notificationSenderService, never()).suspendNotifications(any())
    }

    @Test
    @DisplayName("sendDrinkingReminders(): suspends exhausted notification (attempts == 3)")
    fun `sendDrinkingReminders suspends exhausted notification`() {
        val dto = DtoGenerator.generateNotificationDto(notificationAttempts = 3)
        whenever(notificationSettingsService.getAllNotificationSettings()).thenReturn(setOf(dto))
        doReturn(true).whenever(notificationTimeService).isOutsideQuietTime(dto)
        doReturn(true).whenever(notificationTimeService).isNotificationDue(dto)

        scheduler.sendDrinkingReminders()

        verify(notificationSenderService).suspendNotifications(listOf(dto))
        verify(notificationSenderService, never()).sendNotifications(any())
    }

    @Test
    @DisplayName("sendDrinkingReminders(): partitions into exhausted and active correctly")
    fun `sendDrinkingReminders partitions exhausted and active`() {
        val active = DtoGenerator.generateNotificationDto(notificationAttempts = 1)
        val exhausted = DtoGenerator.generateNotificationDto(notificationAttempts = 3)
        whenever(notificationSettingsService.getAllNotificationSettings()).thenReturn(setOf(active, exhausted))
        doReturn(true).whenever(notificationTimeService).isOutsideQuietTime(active)
        doReturn(true).whenever(notificationTimeService).isOutsideQuietTime(exhausted)
        doReturn(true).whenever(notificationTimeService).isNotificationDue(active)
        doReturn(true).whenever(notificationTimeService).isNotificationDue(exhausted)

        scheduler.sendDrinkingReminders()

        verify(notificationSenderService).sendNotifications(listOf(active))
        verify(notificationSenderService).suspendNotifications(listOf(exhausted))
    }

    @Test
    @DisplayName("sendDrinkingReminders(): throws an error")
    fun `scheduler failed`() {
        whenever(notificationSettingsService.getAllNotificationSettings()).thenThrow(RuntimeException("Scheduler Failed"))

        scheduler.sendDrinkingReminders()

        verify(notificationSenderService, never()).sendNotifications(any())
        verify(notificationSenderService, never()).suspendNotifications(any())
    }
}
