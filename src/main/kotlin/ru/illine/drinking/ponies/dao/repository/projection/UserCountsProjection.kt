package ru.illine.drinking.ponies.dao.repository.projection

interface UserCountsProjection {
    val all: Long
    val active: Long
    val inactive: Long
    val banned: Long
}
