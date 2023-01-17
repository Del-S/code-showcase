package cz.csob.smartbanking.compose.core

/**
 * Annotation to assign a Screenshot test filter to a test. This annotation can be used at a
 * method or class level.
 *
 * @author eMan a.s.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ScreenshotTest

