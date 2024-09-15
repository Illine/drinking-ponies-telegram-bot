package ru.illine.drinking.ponies.service.button.impl

import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.config.property.ButtonProperties
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.service.button.ButtonDataService

@Service
class SettingsButtonDataServiceImpl(
    private val buttonProperties: ButtonProperties
) : ButtonDataService<SettingsType> {

    override fun getData(enumValue: SettingsType): String {
        return when (enumValue) {
            SettingsType.DELAY_NOTIFICATION -> buttonProperties.data.delayNotification
            SettingsType.QUIET_MODE_TIME -> buttonProperties.data.quietModeTime
            SettingsType.TIMEZONE -> buttonProperties.data.timezone
        }
    }
}