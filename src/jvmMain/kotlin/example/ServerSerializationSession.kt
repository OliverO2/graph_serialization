package example

import kotlinx.coroutines.asContextElement

/**
 * A server's serialization session.
 *
 * Serialization on a server generally occurs inside a coroutine representing an individual client's session.
 * This coroutine must be initialized with a context element created by [ServerSerializationSession.coroutineContext].
 * Example:
 * ```
 * withContext(ServerSerializationSession.coroutineContext()) {
 *     ...
 * }
 * ```
 *
 * If only a single session is required, serialization can be initialized via [initializeSingleInstance].
 */
object ServerSerializationSession {
    internal val threadLocalContext = ThreadLocal<SerializationSessionContext?>()

    /** returns the coroutine context (element) establishing a serialization session. */
    fun coroutineContext() = threadLocalContext.asContextElement(value = SerializationSessionContext())

    /** initializes a single serialization session. */
    fun initializeSingleInstance() {
        threadLocalContext.set(SerializationSessionContext())
    }
}

actual fun serializationSessionContext() =
    ServerSerializationSession.threadLocalContext.get()
        ?: throw IllegalStateException(
            "The serialization session's context has not been initialized.\n"
                    + " See 'ServerSerializationSession' for initialization requirements."
        )