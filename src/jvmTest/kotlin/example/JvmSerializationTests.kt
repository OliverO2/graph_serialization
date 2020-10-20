package example

import kotlinx.coroutines.*
import mu.KotlinLogging
import kotlin.test.Test

private val logger = KotlinLogging.logger {}

class JvmSerializationTests : CommonSerializationTests() {
    @Test
    fun `test JVM serialization`() = runBlocking {
        val job =
            launch(Dispatchers.Unconfined + ServerSerializationSession.coroutineContext()) {
                logger.debug("context: ${serializationSessionContext()}")
                test_Json_Serialization()

                delay(1)  // Make the dispatcher switch to another thread. A yield() will not do.

                logger.debug("context: ${serializationSessionContext()}")
                test_Json_Serialization()
            }
        job.join()
    }
}