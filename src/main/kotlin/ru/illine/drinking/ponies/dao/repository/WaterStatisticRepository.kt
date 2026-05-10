package ru.illine.drinking.ponies.dao.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.illine.drinking.ponies.model.entity.WaterStatisticEntity
import java.time.LocalDateTime

interface WaterStatisticRepository : JpaRepository<WaterStatisticEntity, Long> {

    @Query(
        value = """
            select ws from WaterStatisticEntity ws
            join fetch ws.telegramUser u
            where u.externalUserId = :externalUserId
              and ws.eventTime >= :startInclusive
              and ws.eventTime < :endExclusive
            order by ws.eventTime asc
        """
    )
    fun findByUserAndEventTimeBetween(
        @Param("externalUserId") externalUserId: Long,
        @Param("startInclusive") startInclusive: LocalDateTime,
        @Param("endExclusive") endExclusive: LocalDateTime
    ): List<WaterStatisticEntity>

}
