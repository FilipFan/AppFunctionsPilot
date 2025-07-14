package dev.filipfan.appfunctionspilot.agent

import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata

fun List<AppFunctionMetadata>.toFunctionDeclarations(): List<FunctionDeclaration> = this.map {
    it.toFunctionDeclaration()
}

private fun AppFunctionMetadata.toFunctionDeclaration(): FunctionDeclaration = FunctionDeclaration(
    name = this.id,
    shortName = this.schema?.name ?: "Unknown",
    description = "", // Not supported yet.
    parameters = this.toParametersSchema(),
    response = this.response.valueType.toSchema(this.components),
)

private fun AppFunctionMetadata.toParametersSchema(): Schema? {
    if (parameters.isEmpty()) return null

    // Wrap primitive type calls with a single parameter into object type.
    parameters.singleOrNull()
        ?.takeIf { it.dataType is AppFunctionPrimitiveTypeMetadata }
        ?.let { param ->
            val primitiveSchema = (param.dataType as AppFunctionPrimitiveTypeMetadata).toSchema()
            return Schema(
                type = DataType.OBJECT,
                properties = mapOf(param.name to primitiveSchema),
                required = if (param.isRequired) listOf(param.name) else emptyList(),
            )
        }

    return createObjectSchema(parameters, components)
}

private fun createObjectSchema(
    params: List<AppFunctionParameterMetadata>,
    components: AppFunctionComponentsMetadata,
): Schema = Schema(
    type = DataType.OBJECT,
    description = "", // Not supported yet.
    properties = params.associate { param ->
        param.name to param.dataType.toSchema(components)
    },
    required = params.filter { it.isRequired }.map { it.name },
)

private fun AppFunctionDataTypeMetadata.toSchema(
    components: AppFunctionComponentsMetadata,
): Schema = when (this) {
    is AppFunctionPrimitiveTypeMetadata -> this.toSchema()
    is AppFunctionArrayTypeMetadata -> Schema(
        type = DataType.ARRAY,
        description = "", // Not supported yet.
        nullable = this.isNullable,
        items = this.itemType.toSchema(components),
    )

    is AppFunctionObjectTypeMetadata -> Schema(
        type = DataType.OBJECT,
        description = "", // Not supported yet.
        nullable = this.isNullable,
        properties = this.properties.mapValues { (_, value) -> value.toSchema(components) },
        required = this.required,
    )

    is AppFunctionReferenceTypeMetadata -> {
        val resolvedType = components.dataTypes[this.referenceDataType]
            ?: throw IllegalStateException("Reference to ${this.referenceDataType} not found.")
        resolvedType.toSchema(components)
    }

    else -> throw IllegalStateException("Unexpected data type: $this")
}

private fun AppFunctionPrimitiveTypeMetadata.toSchema(): Schema = Schema(
    type = this.toSchemaDataType(),
    description = "", // Not supported yet.
    nullable = this.isNullable,
)

private fun AppFunctionPrimitiveTypeMetadata.toSchemaDataType(): DataType = when (type) {
    AppFunctionPrimitiveTypeMetadata.TYPE_BOOLEAN -> DataType.BOOLEAN
    AppFunctionPrimitiveTypeMetadata.TYPE_LONG -> DataType.LONG
    AppFunctionPrimitiveTypeMetadata.TYPE_DOUBLE -> DataType.DOUBLE
    AppFunctionPrimitiveTypeMetadata.TYPE_FLOAT -> DataType.FLOAT
    AppFunctionPrimitiveTypeMetadata.TYPE_INT -> DataType.INT
    AppFunctionPrimitiveTypeMetadata.TYPE_STRING -> DataType.STRING
    AppFunctionPrimitiveTypeMetadata.TYPE_UNIT -> DataType.UNIT
    else -> throw IllegalStateException("Unexpected primitive data type: $type")
}
