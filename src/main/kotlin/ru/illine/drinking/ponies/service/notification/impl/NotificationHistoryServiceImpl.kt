package ru.illine.drinking.ponies.service.notification.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.exception.NotificationHistoryEntryNotEditableException
import ru.illine.drinking.ponies.exception.NotificationHistoryEntryNotFoundException
import ru.illine.drinking.ponies.model.base.NotificationHistoryStatus
import ru.illine.drinking.ponies.model.base.WaterEntrySourceType
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.model.dto.response.NotificationHistoryDay
import ru.illine.drinking.ponies.model.dto.response.NotificationHistoryEvent
import ru.illine.drinking.ponies.model.dto.response.NotificationHistoryResponse
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
    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun getHistory(
        externalUserId: Long,
        from: LocalDate,
        to: LocalDate,
    ): NotificationHistoryResponse {
        logger.debug("Getting [{} - {}] notification history for telegram user [{}]", from, to, externalUserId)

        require(from <= to) { "Invalid parameter: 'from' must be before or equal to 'to'" }
        require(ChronoUnit.DAYS.between(from, to) < MAX_RANGE_DAYS) {
            "Invalid parameter: the range must not exceed $MAX_RANGE_DAYS days"
        }
        // Without an upper bound, a date near LocalDate.MAX overflows while shifting the end of the range.
        require(to.year < LocalDate.MAX.year) {
            "Invalid parameter: 'to' must not be later than the year ${LocalDate.MAX.year}"
        }
        // Symmetrically, a date near LocalDate.MIN underflows while shifting the start of the range into UTC:
        // that raises a DateTimeException, which is not a rejected request but a server error.
        require(from.year > LocalDate.MIN.year) {
            "Invalid parameter: 'from' must not be earlier than the year ${LocalDate.MIN.year}"
        }

        // A range in the future is legitimate - the client may page the calendar forward, it just gets no days.
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
                .map { (date, events) -> NotificationHistoryDay(date = date, events = events) }
                // Days come out ordered by event time, except where a DST fall-back crosses local midnight.
                .sortedBy { it.date }

        return NotificationHistoryResponse(days = days)
    }

    override fun updateEntry(
        externalUserId: Long,
        entryId: Long,
        status: NotificationHistoryStatus,
        amountMl: Int?,
    ): NotificationHistoryEvent {
        logger.info(
            "Updating notification history entry [{}] of telegram user [{}] to status [{}]",
            entryId,
            externalUserId,
            status,
        )

        // A missed entry never holds a volume, so the client's snapshot of the form is ignored for it.
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

        // An entry the journal never shows (foreign, manual or snoozed) is reported as missing:
        // we do not confirm that it exists.
        val entry =
            waterStatisticAccessService
                .findByIdAndUser(entryId, externalUserId)
                ?.takeIf { journalStatus(it) != null }
                ?: throw NotificationHistoryEntryNotFoundException(
                    "Not found a notification history entry by id [$entryId] of externalUserId [$externalUserId]",
                )

        // The entry is fetched with its owner, so the timezone comes from it instead of a second lookup.
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

    // Single answer to "is this record a journal entry at all", shared by reading and editing.
    private fun journalStatus(dto: WaterStatisticDto): NotificationHistoryStatus? =
        if (dto.source in JOURNAL_SOURCES) NotificationHistoryStatus.of(dto.eventType) else null

    // The single predicate behind both the 'editable' flag of the journal and the refusal to update.
    // The window spans whole calendar dates of the user's timezone, so all entries of one local day
    // always share the same flag: the client draws a single lock per day.
    private fun editWindow(zone: ZoneId): (Instant) -> Boolean {
        val startDate = LocalDate.now(clock.withZone(zone)).minusDays(EDIT_WINDOW_DAYS)
        return { eventTime -> !eventTime.atZone(zone).toLocalDate().isBefore(startDate) }
    }

    private fun toEvent(
        dto: WaterStatisticDto,
        isEditable: (Instant) -> Boolean,
    ): NotificationHistoryEvent {
        val eventTime = dto.eventTime.toUtcInstant()
        return NotificationHistoryEvent(
            id = checkNotNull(dto.id) { "A persisted water statistic record must have an id" },
            eventTime = eventTime,
            status = checkNotNull(journalStatus(dto)) { "A journal entry must carry a journal status" },
            amountMl = dto.waterAmountMl,
            source = dto.source,
            editable = isEditable(eventTime),
        )
    }

    companion object {
        // Calendar days of the user's timezone: the current local day and that many days before it stay editable.
        const val EDIT_WINDOW_DAYS = 7L
        const val MAX_RANGE_DAYS = 62L

        // Widening the journal to another source is a single addition here: both reading and editing follow it.
        val JOURNAL_SOURCES = setOf(WaterEntrySourceType.NOTIFICATION)
        val JOURNAL_EVENT_TYPES = NotificationHistoryStatus.entries.map { it.eventType }

        private val AMOUNT_RANGE = WaterEntryConstants.MIN_ML.toInt()..WaterEntryConstants.MAX_ML.toInt()
    }
}
