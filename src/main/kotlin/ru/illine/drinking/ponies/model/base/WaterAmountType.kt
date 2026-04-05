package ru.illine.drinking.ponies.model.base

import ru.illine.drinking.ponies.util.telegram.TelegramWaterAmountConstants
import java.util.*

@Suppress("unused")
enum class WaterAmountType(
    val displayName: String,
    val amountMl: Int,
    val queryData: UUID
) {
    ML_50(TelegramWaterAmountConstants.ML_50, 50, UUID.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d")),
    ML_100(TelegramWaterAmountConstants.ML_100, 100, UUID.fromString("b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e")),
    ML_150(TelegramWaterAmountConstants.ML_150, 150, UUID.fromString("c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f")),
    ML_250(TelegramWaterAmountConstants.ML_250, 250, UUID.fromString("d4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a")),
    ML_450(TelegramWaterAmountConstants.ML_450, 450, UUID.fromString("e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8a9b"));

    companion object {

        fun typeOf(queryData: String): WaterAmountType? {
            return WaterAmountType.entries
                .find { Objects.equals(queryData, it.queryData.toString()) }
        }

    }

}
