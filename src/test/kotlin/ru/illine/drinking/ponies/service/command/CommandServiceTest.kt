package ru.illine.drinking.ponies.service.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonWebApp
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.service.command.impl.CommandServiceImpl
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMenuConstants

@UnitTest
@DisplayName("CommandService Unit Test")
class CommandServiceTest {

    private lateinit var sender: TelegramClient

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
    }

    @Test
    @DisplayName("register(): autoUpdateTelegramConfig=true - registers menu button")
    fun `register when autoUpdateTelegramConfig is true`() {
        val service = buildService(autoUpdateTelegramConfig = true)

        service.register()

        val menuCaptor = ArgumentCaptor.forClass(SetChatMenuButton::class.java)
        verify(sender).execute(menuCaptor.capture())

        val menuButton = menuCaptor.value.menuButton as MenuButtonWebApp
        assertEquals(TelegramMenuConstants.MENU_BUTTON_TEXT, menuButton.text)
        assertEquals(MINI_APP_URL, menuButton.webAppInfo.url)
    }

    @Test
    @DisplayName("register(): autoUpdateTelegramConfig=false - no interactions with sender")
    fun `register when autoUpdateTelegramConfig is false`() {
        val service = buildService(autoUpdateTelegramConfig = false)

        service.register()

        verifyNoInteractions(sender)
    }

    private fun buildService(autoUpdateTelegramConfig: Boolean): CommandServiceImpl {
        val properties = TelegramBotProperties(
            token = "token",
            username = "username",
            miniAppUrl = MINI_APP_URL,
            autoUpdateTelegramConfig = autoUpdateTelegramConfig,
            http = TelegramBotProperties.Http(connectionTimeToLiveInSec = 30, maxConnectionTotal = 10)
        )
        return CommandServiceImpl(properties, sender)
    }

    private companion object {
        const val MINI_APP_URL = "https://t.me/Test/app"
    }
}
