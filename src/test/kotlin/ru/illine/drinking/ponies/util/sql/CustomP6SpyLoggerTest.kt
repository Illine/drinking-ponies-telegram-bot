package ru.illine.drinking.ponies.util.sql

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.springframework.util.ReflectionUtils
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("CustomP6SpyLogger Unit Test")
class CustomP6SpyLoggerTest {

    @Test
    @DisplayName("init: initializes without exception when field is found")
    fun `init succeeds when field found`() {
        CustomP6SpyLogger()
    }

    @Test
    @DisplayName("init: does nothing when field is not found")
    fun `init skips when field not found`() {
        mockStatic(ReflectionUtils::class.java).use { mocked: MockedStatic<ReflectionUtils> ->
            mocked.`when`<Any?> { ReflectionUtils.findField(any(), anyString()) }.thenReturn(null)

            CustomP6SpyLogger()
        }
    }
}
