package ru.illine.drinking.ponies.service.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.model.base.TelegramCommandType
import ru.illine.drinking.ponies.service.command.impl.CommandServiceImpl
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("CommandService Unit Test")
class CommandServiceTest {

    private lateinit var sender: TelegramClient
    private lateinit var properties: TelegramBotProperties

    private fun buildService(autoUpdateCommands: Boolean): CommandServiceImpl {
        properties = TelegramBotProperties(
            version = "1.0.0",
            token = "token",
            username = "username",
            creatorId = 1L,
            autoUpdateCommands = autoUpdateCommands,
            http = TelegramBotProperties.Http(connectionTimeToLiveInSec = 30, maxConnectionTotal = 10)
        )
        return CommandServiceImpl(properties, sender)
    }

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
    }

    @Test
    @DisplayName("register(): autoUpdateCommands=true - executes SetMyCommands with only visible commands sorted by order")
    fun `register when autoUpdateCommands is true`() {
        val service = buildService(autoUpdateCommands = true)
        val expectedCommands = TelegramCommandType.entries
            .filter { it.visible }
            .sortedBy { it.order }
            .map { it.command }

        service.register()

        val captor = ArgumentCaptor.forClass(SetMyCommands::class.java)
        verify(sender).execute(captor.capture())
        val actualCommands = captor.value.commands.map { it.command }
        assertEquals(expectedCommands, actualCommands)
    }

    @Test
    @DisplayName("register(): autoUpdateCommands=false - no interactions with sender")
    fun `register when autoUpdateCommands is false`() {
        val service = buildService(autoUpdateCommands = false)

        service.register()

        verifyNoInteractions(sender)
    }
}