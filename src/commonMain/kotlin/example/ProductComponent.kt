package example

import kotlinx.serialization.*
import kotlinx.serialization.modules.*

@Serializable
abstract class ProductComponent : CrossSystemObject()

@Serializable
data class SimpleProductComponent(override val objectID: CrossSystemObjectID, var value: String) : ProductComponent()

@Serializable
data class AggregateProductComponent(
    override val objectID: CrossSystemObjectID,
    @Serializable(with = CrossSystemReferenceSerializer::class)
    var element1: ProductComponent? = null,
    @Serializable(with = CrossSystemReferenceSerializer::class)
    var element2: ProductComponent? = null
) : ProductComponent()

val productSerializersModule = SerializersModule {
    polymorphic(CrossSystemReference::class) {
        subclass(SimpleProductComponent::class)
        subclass(AggregateProductComponent::class)
    }
}