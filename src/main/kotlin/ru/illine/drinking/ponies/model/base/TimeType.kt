package ru.illine.drinking.ponies.model.base

import java.util.*

@Suppress("unused")
enum class TimeType(
    val displayName: String,
    val time: Boolean,
    val queryData: UUID
) {
    DISABLED("Отключение тихого режима", false, UUID.fromString("2f41550e-9a36-4aea-b7d1-485c88285066")),
    T00("00:00", true, UUID.fromString("5e64dcd7-a8e4-49c2-9657-40b4f0baa11f")),
    T01("01:00", true, UUID.fromString("d2887a5e-6c7c-4a8e-bf3f-3e0788b6f7de")),
    T02("02:00", true, UUID.fromString("a1f5d9b7-838d-4e47-9c1a-cc6b089b5a64")),
    T03("03:00", true, UUID.fromString("5b3f8c99-8c3d-4b95-9275-6a7e8125bda9")),
    T04("04:00", true, UUID.fromString("84a8b8f4-4c88-4e4c-a96f-1a7c0e924b67")),
    T05("05:00", true, UUID.fromString("9e8f0b5e-b4b7-4e93-80f0-3a3df2b3e8de")),
    T06("06:00", true, UUID.fromString("d7387f7b-3e8a-4d6e-8f09-6f0c4f8d3f29")),
    T07("07:00", true, UUID.fromString("e9b7c5e8-f5f9-4e39-91f0-8f5f5a8b3c8d")),
    T08("08:00", true, UUID.fromString("a6c7d8e8-7e9f-4e7b-91d0-3f0c9b5e7f8d")),
    T09("09:00", true, UUID.fromString("c4b7e6f8-3a9e-4e8a-91e9-5f7b9f8d3a8c")),
    T10("10:00", true, UUID.fromString("d6e7a9f8-7b6e-4e8a-9e0b-6f7d8e9f0a9b")),
    T11("11:00", true, UUID.fromString("b8c6d7e9-9e7b-4e6d-80a9-7e6c9f8d3a7b")),
    T12("12:00", true, UUID.fromString("d9e8a0b6-5e7c-4e9a-9f0b-6a7d8c9e5b8f")),
    T13("13:00", true, UUID.fromString("a7e9d8c5-4e7b-4e8a-9d0b-5f7e9f8d2b7c")),
    T14("14:00", true, UUID.fromString("b6e8c9d5-7a9e-4e8b-91d0-4a7e8d9f5c8b")),
    T15("15:00", true, UUID.fromString("e7d8a9f5-3b9e-4e7b-91e0-5a8d9f7c6a9b")),
    T16("16:00", true, UUID.fromString("c7e9a8d5-8e7b-4e9a-91f0-7e8d6f5a4c7b")),
    T17("17:00", true, UUID.fromString("d8c7a9f6-7e8b-4e9a-91e0-6a8d7e5b3c9f")),
    T18("18:00", true, UUID.fromString("b9e8a7d6-6e9b-4e9a-91f0-4e7d9c5a8b7c")),
    T19("19:00", true, UUID.fromString("e8c9a6d7-7e9b-4e8a-91f0-5a8d7e9f6c7b")),
    T20("20:00", true, UUID.fromString("d9e7c6a5-9e7b-4e8a-91f0-7e8d6a5b8c7d")),
    T21("21:00", true, UUID.fromString("b7e8a9d6-5e9b-4e9a-91f0-6e7d8f9c3a5b")),
    T22("22:00", true, UUID.fromString("e9d8a7c5-4e9b-4e9a-91f0-5a7d8c9f6e8b")),
    T23("23:00", true, UUID.fromString("d8c9a6e5-7e9b-4e9a-91f0-4e7d9f6a8b7c"));

    companion object {

        fun typeOf(queryData: String): TimeType? {
            return TimeType.values()
                .find { Objects.equals(queryData, it.queryData.toString()) }
        }

    }

}