package example

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private val logger = KotlinLogging.logger {}

open class CommonSerializationTests {
    init {
        initializeTests()
    }

    private val simpleProductComponent1 = SimpleProductComponent(CrossSystemObjectID("#s1"), "simple component 1")
    private val simpleProductComponent2 = SimpleProductComponent(CrossSystemObjectID("#s2"), "simple component 2")

    private val aggregateProductComponent1 =
        AggregateProductComponent(CrossSystemObjectID("#a1"), simpleProductComponent1)

    private val aggregateProductComponent2 =
        AggregateProductComponent(CrossSystemObjectID("#a2"), aggregateProductComponent1, simpleProductComponent2)

    private val aggregateProductComponent3 =
        AggregateProductComponent(CrossSystemObjectID("#a3"), simpleProductComponent2)

    private val topComponent =
        AggregateProductComponent(CrossSystemObjectID("#top"), aggregateProductComponent2, aggregateProductComponent3)

    private fun CrossSystemObject.assertDeepEquality(
        other: CrossSystemObject?,
        indent: String = "",
        visited: MutableSet<CrossSystemObjectID> = mutableSetOf()
    ) {
        if (objectID in visited)
            logger.debug { "${indent}already seen: $objectID" }
        else {
            visited.add(objectID)
            logger.debug { "${indent}comparing $objectID" }
            assertEquals(this, other)
            if (this is AggregateProductComponent) {
                assertTrue(other is AggregateProductComponent)
                element1?.assertDeepEquality(other.element1, "$indent  ", visited)
                element2?.assertDeepEquality(other.element2, "$indent  ", visited)
            }
        }
    }

    @Test
    fun test_Json_Serialization() {
        aggregateProductComponent1.element2 = null

        Json { serializersModule = productSerializersModule }.run {
            val encodedComponent = encodeToString(topComponent)
            val decodedComponent = decodeFromString<AggregateProductComponent>(encodedComponent)
            decodedComponent.assertDeepEquality(topComponent)
        }
    }

    @Test
    fun test_Json_Cycle_Serialization() {
        serializationSessionContext().run {
            // clear the context's state resulting from a previous test
            serializedObjectIDs.clear()
            deserializedObjects.clear()
        }
        aggregateProductComponent1.element2 = aggregateProductComponent2

        Json { serializersModule = productSerializersModule }.run {
            val encodedComponent = encodeToString(topComponent)
            assertFailsWith<DeserializationCycleException> {
                decodeFromString<AggregateProductComponent>(encodedComponent)
            }
        }
    }
}