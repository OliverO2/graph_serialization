package example

import mu.KotlinLogging
import mu.KotlinLoggingConfiguration
import mu.KotlinLoggingLevel

private val logger = KotlinLogging.logger {}


actual fun initializeTests() {
    KotlinLoggingConfiguration.LOG_LEVEL = KotlinLoggingLevel.DEBUG
    logger.debug { "initializing tests" }
}