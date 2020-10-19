package example

import kotlinx.serialization.*
import kotlinx.serialization.modules.*

@Serializable
abstract class ProductComponent : CrossSystemObject()

@Serializable
data class SimpleProductComponent(override val objectID: CrossSystemObjectID, var value: String) : ProductComponent()

class ProductComponentReferenceSerializer: CrossSystemReferenceSerializer<ProductComponent>()

@Serializable
data class AggregateProductComponent(
    override val objectID: CrossSystemObjectID,
    @Serializable(with = ProductComponentReferenceSerializer::class)
    var element1: ProductComponent? = null,
    @Serializable(with = ProductComponentReferenceSerializer::class)
    var element2: ProductComponent? = null
) : ProductComponent()

val productSerializersModule = SerializersModule {
    polymorphic(CrossSystemReference::class) {
        subclass(SimpleProductComponent::class)
        subclass(AggregateProductComponent::class)
    }
}