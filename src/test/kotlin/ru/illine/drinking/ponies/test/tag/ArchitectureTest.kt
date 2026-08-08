package ru.illine.drinking.ponies.test.tag

import org.junit.jupiter.api.Tag

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("architecture")
annotation class ArchitectureTest
