package ru.illine.drinking.ponies.scheduler

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.service.notification.NotificationService
import ru.illine.drinking.ponies.service.notification.NotificationTimeService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("NotificationScheduler Unit Test")
class NotificationSchedulerTest {

    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var notificationService: NotificationService
    private lateinit var notificationTimeService: NotificationTimeService
    private lateinit var scheduler: NotificationScheduler

    @BeforeEach
    fun setUp() {
        notificationAccessService = mock(NotificationAccessService::class.java)
        notificationService = mock(NotificationService::class.java)
        notificationTimeService = mock(NotificationTimeService::class.java)
        scheduler = NotificationScheduler(notificationAccessService, notificationService, notificationTimeService)
    }

    @Test
    @DisplayName("sendDrinkingReminders(): no interactions when no notifications exist")
    fun `sendDrinkingReminders does nothing when no notifications`() {
        `when`(notificationAccessService.findAllNotificationSettings()).thenReturn(emptySet())

        scheduler.sendDrinkingReminders()

        verify(notificationService, never()).sendNotifications(anyCollection())
        verify(notificationService, never()).suspendNotifications(anyCollection())
    }

    @Test
    @DisplayName("sendDrinkingReminders(): skips disabled notifications")
    fun `sendDrinkingReminders filters out disabled notifications`() {
        val disabled = DtoGenerator.generateNotificationDto().copy(enabled = false)
        `when`(notificationAccessService.findAllNotificationSettings()).thenReturn(setOf(disabled))

        scheduler.sendDrinkingReminders()

        verify(notificationService, never()).sendNotifications(anyCollection())
        verify(notificationService, never()).suspendNotifications(anyCollection())
    }

    @Test
    @DisplayName("sendDrinkingReminders(): skips notifications inside quiet time")
    fun `sendDrinkingReminders filters out quiet time notifications`() {
        val dto = DtoGenerator.generateNotificationDto()
        `when`(notificationAccessService.findAllNotificationSettings()).thenReturn(setOf(dto))
        doReturn(false).`when`(notificationTimeService).isOutsideQuietTime(dto)

        scheduler.sendDrinkingReminders()

        verify(notificationService, never()).sendNotifications(anyCollection())
        verify(notificationService, never()).suspendNotifications(anyCollection())
    }

    @Test
    @DisplayName("sendDrinkingReminders(): skips notifications not yet due")
    fun `sendDrinkingReminders filters out not due notifications`() {
        val dto = DtoGenerator.generateNotificationDto()
        `when`(notificationAccessService.findAllNotificationSettings()).thenReturn(setOf(dto))
        doReturn(true).`when`(notificationTimeService).isOutsideQuietTime(dto)
        doReturn(false).`when`(notificationTimeService).isNotificationDue(dto)

        scheduler.sendDrinkingReminders()

        verify(notificationService, never()).sendNotifications(anyCollection())
        verify(notificationService, never()).suspendNotifications(anyCollection())
    }

    @Test
    @DisplayName("sendDrinkingReminders(): sends active notification (attempts < 3)")
    fun `sendDrinkingReminders sends active notification`() {
        val dto = DtoGenerator.generateNotificationDto(notificationAttempts = 0)
        `when`(notificationAccessService.findAllNotificationSettings()).thenReturn(setOf(dto))
        doReturn(true).`when`(notificationTimeService).isOutsideQuietTime(dto)
        doReturn(true).`when`(notificationTimeService).isNotificationDue(dto)

        scheduler.sendDrinkingReminders()

        verify(notificationService).sendNotifications(listOf(dto))
        verify(notificationService, never()).suspendNotifications(anyCollection())
    }

    @Test
    @DisplayName("sendDrinkingReminders(): suspends exhausted notification (attempts == 3)")
    fun `sendDrinkingReminders suspends exhausted notification`() {
        val dto = DtoGenerator.generateNotificationDto(notificationAttempts = 3)
        `when`(notificationAccessService.findAllNotificationSettings()).thenReturn(setOf(dto))
        doReturn(true).`when`(notificationTimeService).isOutsideQuietTime(dto)
        doReturn(true).`when`(notificationTimeService).isNotificationDue(dto)

        scheduler.sendDrinkingReminders()

        verify(notificationService).suspendNotifications(listOf(dto))
        verify(notificationService, never()).sendNotifications(anyCollection())
    }

    @Test
    @DisplayName("sendDrinkingReminders(): partitions into exhausted and active correctly")
    fun `sendDrinkingReminders partitions exhausted and active`() {
        val active = DtoGenerator.generateNotificationDto(notificationAttempts = 1)
        val exhausted = DtoGenerator.generateNotificationDto(notificationAttempts = 3)
        `when`(notificationAccessService.findAllNotificationSettings()).thenReturn(setOf(active, exhausted))
        doReturn(true).`when`(notificationTimeService).isOutsideQuietTime(active)
        doReturn(true).`when`(notificationTimeService).isOutsideQuietTime(exhausted)
        doReturn(true).`when`(notificationTimeService).isNotificationDue(active)
        doReturn(true).`when`(notificationTimeService).isNotificationDue(exhausted)

        scheduler.sendDrinkingReminders()

        verify(notificationService).sendNotifications(listOf(active))
        verify(notificationService).suspendNotifications(listOf(exhausted))
    }
}
