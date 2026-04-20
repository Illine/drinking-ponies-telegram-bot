package ru.illine.drinking.ponies.service.button

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.config.property.ButtonProperties
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.service.button.impl.SettingsButtonDataServiceImpl
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("SettingsButtonDataService Unit Test")
class SettingsButtonDataServiceTest {

    private val notificationIntervalUrl = "http://example.com/interval"
    private val quietModeTimeUrl = "http://example.com/quiet"
    private val timezoneUrl = "http://example.com/timezone"

    private lateinit var service: ButtonDataService<SettingsType>

    @BeforeEach
    fun setUp() {
        val properties = ButtonProperties(
            data = ButtonProperties.Data(
                notificationInterval = notificationIntervalUrl,
                quietModeTime = quietModeTimeUrl,
                timezone = timezoneUrl
            )
        )
        service = SettingsButtonDataServiceImpl(properties)
    }

    @Test
    @DisplayName("getData(): NOTIFICATION_INTERVAL - returns notificationInterval url")
    fun `getData returns notificationInterval url`() {
        assertEquals(notificationIntervalUrl, service.getData(SettingsType.NOTIFICATION_INTERVAL))
    }

    @Test
    @DisplayName("getData(): QUIET_MODE_TIME - returns quietModeTime url")
    fun `getData returns quietModeTime url`() {
        assertEquals(quietModeTimeUrl, service.getData(SettingsType.QUIET_MODE_TIME))
    }

    @Test
    @DisplayName("getData(): TIMEZONE - returns timezone url")
    fun `getData returns timezone url`() {
        assertEquals(timezoneUrl, service.getData(SettingsType.TIMEZONE))
    }
}
