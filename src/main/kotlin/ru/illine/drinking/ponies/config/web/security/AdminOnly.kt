package ru.illine.drinking.ponies.config.web.security

// On a class it guards every handler of that controller, so a new admin endpoint cannot
// be left open by forgetting the annotation. On a function it guards that handler alone.
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AdminOnly
