package example

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * A cross-system object reference.
 */
@Serializable
sealed class CrossSystemReference

/** A unique object identifier, which is a cross-system reference in its own right. */
@Serializable
data class CrossSystemObjectID(val value: String) : CrossSystemReference() {
    override fun toString() = "[$value]"
}

/** A cross-system referenceable object, also considered to be a reference to itself. */
@Serializable
abstract class CrossSystemObject : CrossSystemReference() {
    abstract val objectID: CrossSystemObjectID
}

/**
 * A serializer for a nullable cross-system object reference.
 *
 * To serialize a property of type `MyObject` being a subtype of [CrossSystemObject],
 * 1. define a serializer like this:
 *     ```
 *     class MyObjectReferenceSerializer: CrossSystemReferenceSerializer<MyObject>()
 *     ```
 * 2. annotate the property like this:
 *     ```
 *     @Serializable(with = MyObjectReferenceSerializer::class)
 *     var myProperty: MyObject? = null
 *     ```
 *
 * This serializer provides compression of repeated references by serializing an objects contents only once, correctly
 * initializing repeated references to the same object.
 * The [serialize] method handles cycles in the object graph which would otherwise result in a stack overflow.
 * TODO: the [deserialize] method does not yet deal correctly with cycles.
 */
open class CrossSystemReferenceSerializer<Type : CrossSystemObject> : KSerializer<Type?> {

    /**
     * A [DeserializationCycleException] occurs when deserializing a descendant object referencing to some ancestor.
     * As the ancestor object will be available only after its complete deserialization, it will be missing when a
     * descendant's reference needs it.
     */
    class DeserializationCycleException(message: String) : SerializationException(message)

    companion object {
        var sessionContext = SerializationSessionContext()
        // TODO: use https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/coroutine-context-and-dispatchers.md#thread-local-data

        private val baseSerializer = CrossSystemReference.serializer().nullable
        // TODO: Commenting out the preceding line and uncommenting the following line will fail with the IR backend on JS
        //      private val baseSerializer = serializer<CrossSystemReference>().nullable
        //      With the line above, running the jsTest task produces
        //          SerializationException: Serializer for class 'CrossSystemReference' is not found.
        //          Mark the class as @Serializable or provide the serializer explicitly.
        //          On Kotlin/JS explicitly declared serializer should be used for interfaces and enums without @Serializable annotation
    }

    override val descriptor: SerialDescriptor = baseSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Type?) {
        when {
            value == null -> {
                baseSerializer.serialize(encoder, null)
            }

            value.objectID in sessionContext.serializedObjectIDs -> {
                logger.debug { "serialize ${value.objectID}: repeated reference, serializing ID only" }
                baseSerializer.serialize(encoder, value.objectID)
            }

            else -> {
                logger.debug { "serialize ${value.objectID}: initial reference, serializing object, adding to serialization cache" }
                sessionContext.serializedObjectIDs.add(value.objectID)
                baseSerializer.serialize(encoder, value)
            }
        }
    }

    override fun deserialize(decoder: Decoder): Type? {
        when (val objectReference = baseSerializer.deserialize(decoder)) {
            null -> return null

            is CrossSystemObject -> {
                logger.debug { "deserialize ${objectReference.objectID}: initial reference, deserializing object, adding to deserialization cache" }
                sessionContext.deserializedObjects[objectReference.objectID] = objectReference
                @Suppress("UNCHECKED_CAST")
                return objectReference as Type
            }

            is CrossSystemObjectID -> {
                logger.debug { "deserialize $objectReference: repeated reference, obtaining from deserialization cache" }
                if (objectReference in sessionContext.deserializedObjects) {
                    @Suppress("UNCHECKED_CAST")
                    return sessionContext.deserializedObjects[objectReference] as Type
                } else {
                    throw DeserializationCycleException(
                        "Object $objectReference is unavailable in cache due to a descendant referring to an ancestor."
                    )
                }
            }
        }
    }
}

/**
 * A session context governing the life-cycle of a serializable graph.
 *
 * Server-side use:
 *
 * A server typically deals with a larger object graph, portions of which have been shared with different clients.
 * To distinguish, which objects are available ob which client, the server maintains a session context per client.
 *
 * Client-side use:
 *
 * A client communicating with only one server maintains just a single session context.
 */
class SerializationSessionContext {
    // IDs of serialized objects.
    internal val serializedObjectIDs = mutableSetOf<CrossSystemObjectID>()

    // References to deserialized objects.
    internal val deserializedObjects = mutableMapOf<CrossSystemObjectID, CrossSystemObject>()
}