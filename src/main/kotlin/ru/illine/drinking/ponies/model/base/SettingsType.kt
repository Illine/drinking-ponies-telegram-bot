package ru.illine.drinking.ponies.model.base

import ru.illine.drinking.ponies.service.button.GetterButtonData

@Suppress("unused")
enum class SettingsType(
    val displayName: String,
    val getterData: GetterButtonData<SettingsType>,
    val visible: Boolean,
    val web: Boolean
) {
    DELAY_NOTIFICATION(
        "Периодичность уведомлений",
        GetterButtonData { service -> service.getData(DELAY_NOTIFICATION) },
        true,
        false
    ),
    QUIET_MODE_TIME(
        "Расписание тихого режима",
        GetterButtonData { buttonService -> buttonService.getData(QUIET_MODE_TIME) },
        true,
        true
    ),
    TIMEZONE(
        "Часовой пояс",
        GetterButtonData { buttonService -> buttonService.getData(TIMEZONE) },
        false,
        false
    )
}
