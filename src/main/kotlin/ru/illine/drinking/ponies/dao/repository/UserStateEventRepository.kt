package ru.illine.drinking.ponies.dao.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.illine.drinking.ponies.model.entity.UserStateEventEntity

interface UserStateEventRepository : JpaRepository<UserStateEventEntity, Long>
