package ru.illine.drinking.ponies.service.notification.impl

import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.exception.NotificationHistoryEntryNotEditableException
import ru.illine.drinking.ponies.exception.NotificationHistoryEntryNotFoundException
import ru.illine.drinking.ponies.model.base.AppLogger
import ru.illine.drinking.ponies.model.base.NotificationHistoryStatus
import ru.illine.drinking.ponies.model.base.WaterEntrySourceType
import ru.illine.drinking.ponies.model.dto.internal.NotificationHistoryDayDto
import ru.illine.drinking.ponies.model.dto.internal.NotificationHistoryDto
import ru.illine.drinking.ponies.model.dto.internal.NotificationHistoryEventDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.service.notification.NotificationHistoryService
import ru.illine.drinking.ponies.util.statistics.StatisticsPeriodHelper
import ru.illine.drinking.ponies.util.statistics.toUtcInstant
import ru.illine.drinking.ponies.util.water.WaterEntryConstants
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class NotificationHistoryServiceImpl(
    private val notificationAccessService: NotificationAccessService,
    private val waterStatisticAccessService: WaterStatisticAccessService,
    private val clock: Clock,
) : NotificationHistoryService {
    private val logger = AppLogger.SERVICE.logger

    override fun getHistory(
        externalUserId: Long,
        from: LocalDate,
        to: LocalDate,
    ): NotificationHistoryDto {
        logger.debug("Getting [{} - {}] notification history for telegram user [{}]", from, to, externalUserId)

        require(from <= to) { "Invalid parameter: 'from' must be before or equal to 'to'" }
        require(ChronoUnit.DAYS.between(from, to) < MAX_RANGE_DAYS) {
            "Invalid parameter: the range must not exceed $MAX_RANGE_DAYS days"
        }
        require(to.year < LocalDate.MAX.year) {
            "Invalid parameter: 'to' must not be later than the year ${LocalDate.MAX.year}"
        }
        require(from.year > LocalDate.MIN.year) {
            "Invalid parameter: 'from' must not be earlier than the year ${LocalDate.MIN.year}"
        }

        val zone = userZone(externalUserId)
        val (startInclusive, endExclusive) =
            StatisticsPeriodHelper.localDayBoundsToUtc(
                from,
                to.plusDays(1),
                zone,
            )
        val isEditable = editWindow(zone)

        val days =
            waterStatisticAccessService
                .findByUserAndTypesAndEventTimeBetween(
                    externalUserId,
                    JOURNAL_SOURCES,
                    JOURNAL_EVENT_TYPES,
                    startInclusive,
                    endExclusive,
                ).map { toEvent(it, isEditable) }
                .groupBy { it.eventTime.atZone(zone).toLocalDate() }
                .map { (date, events) -> NotificationHistoryDayDto(date = date, events = events) }
                .sortedBy { it.date }

        return NotificationHistoryDto(days = days)
    }

    override fun updateEntry(
        externalUserId: Long,
        entryId: Long,
        status: NotificationHistoryStatus,
        amountMl: Int?,
    ): NotificationHistoryEventDto {
        logger.info(
            "Updating notification history entry [{}] of telegram user [{}] to status [{}]",
            entryId,
            externalUserId,
            status,
        )

        val newAmountMl =
            if (status == NotificationHistoryStatus.CONFIRMED) {
                val confirmedAmountMl =
                    requireNotNull(amountMl) { "Invalid parameter: 'amountMl' is required when status is CONFIRMED" }
                require(confirmedAmountMl in AMOUNT_RANGE) {
                    "Invalid parameter: 'amountMl' must be within $AMOUNT_RANGE"
                }
                confirmedAmountMl
            } else {
                0
            }

        val entry =
            waterStatisticAccessService
                .findByIdAndUser(entryId, externalUserId)
                ?.takeIf { journalStatus(it) != null }
                ?: throw NotificationHistoryEntryNotFoundException(
                    "Not found a notification history entry by id [$entryId] of externalUserId [$externalUserId]",
                )

        val isEditable = editWindow(ZoneId.of(entry.telegramUser.userTimeZone))
        if (!isEditable(entry.eventTime.toUtcInstant())) {
            throw NotificationHistoryEntryNotEditableException(
                "The notification history entry [$entryId] is older than $EDIT_WINDOW_DAYS calendar days",
            )
        }

        val saved =
            waterStatisticAccessService.save(
                entry.copy(eventType = status.eventType, waterAmountMl = newAmountMl),
            )
        return toEvent(saved, isEditable)
    }

    private fun userZone(externalUserId: Long): ZoneId {
        val settings = notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)
        return ZoneId.of(settings.telegramUser.userTimeZone)
    }

    private fun journalStatus(dto: WaterStatisticDto): NotificationHistoryStatus? =
        if (dto.source in JOURNAL_SOURCES) NotificationHistoryStatus.of(dto.eventType) else null

    private fun editWindow(zone: ZoneId): (Instant) -> Boolean {
        val startDate = LocalDate.now(clock.withZone(zone)).minusDays(EDIT_WINDOW_DAYS)
        return { eventTime -> !eventTime.atZone(zone).toLocalDate().isBefore(startDate) }
    }

    private fun toEvent(
        dto: WaterStatisticDto,
        isEditable: (Instant) -> Boolean,
    ): NotificationHistoryEventDto {
        val eventTime = dto.eventTime.toUtcInstant()
        return NotificationHistoryEventDto(
            id = checkNotNull(dto.id) { "A persisted water statistic record must have an id" },
            eventTime = eventTime,
            status = checkNotNull(journalStatus(dto)) { "A journal entry must carry a journal status" },
            amountMl = dto.waterAmountMl,
            source = dto.source,
            editable = isEditable(eventTime),
        )
    }

    companion object {
        const val EDIT_WINDOW_DAYS = 7L
        const val MAX_RANGE_DAYS = 62L

        val JOURNAL_SOURCES = setOf(WaterEntrySourceType.NOTIFICATION)
        val JOURNAL_EVENT_TYPES = NotificationHistoryStatus.entries.map { it.eventType }

        private val AMOUNT_RANGE = WaterEntryConstants.MIN_ML.toInt()..WaterEntryConstants.MAX_ML.toInt()
    }
}
