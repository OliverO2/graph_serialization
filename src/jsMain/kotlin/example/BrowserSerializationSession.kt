package example

/**
 * A browser client's only serialization session.
 *
 * (De)serialization can be used directly without further initialization.
 */
private object BrowserSerializationSession {
    val context = SerializationSessionContext()
}

actual fun serializationSessionContext() = BrowserSerializationSession.context