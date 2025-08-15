package dev.filipfan.appfunctionspilot.agent

import com.google.gson.GsonBuilder

data class FunctionDeclaration(
    val name: String,
    val shortName: String,
    val description: String,
    val parameters: Schema? = null,
    val response: Schema? = null,
) {
    /**
     * Converts a FunctionDeclaration object into an OpenAI-style JSON string.
     * See https://platform.openai.com/docs/guides/function-calling#defining-functions.
     */
    fun toJsonString(): String {
        val declarationMap = mutableMapOf<String, Any>(
            "name" to this.name,
            "description" to this.description,
        )

        // If 'parameters' exists, convert it recursively and add it.
        this.parameters?.let {
            declarationMap["parameters"] = convertSchemaToMap(it)
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(declarationMap)
    }

    private fun convertSchemaToMap(schema: Schema): Map<String, Any> {
        val schemaMap = mutableMapOf<String, Any>()

        val jsonType = when (schema.type) {
            DataType.OBJECT -> "object"
            DataType.STRING -> "string"
            DataType.ARRAY -> "array"
            DataType.BOOLEAN -> "boolean"
            DataType.INT, DataType.LONG -> "integer"
            DataType.FLOAT, DataType.DOUBLE -> "number"
            // UNSPECIFIED and UNIT types are ignored in the final JSON.
            // See https://json-schema.org/understanding-json-schema/reference/type.
            else -> null
        }

        jsonType?.let { schemaMap["type"] = it }

        if (schema.description.isNotBlank()) {
            schemaMap["description"] = schema.description
        }

        if (schema.enum.isNotEmpty()) {
            schemaMap["enum"] = schema.enum
        }

        // For an OBJECT, recursively convert its properties and add the 'required' list.
        if (schema.type == DataType.OBJECT) {
            if (schema.properties.isNotEmpty()) {
                schemaMap["properties"] = schema.properties.mapValues { entry ->
                    convertSchemaToMap(entry.value)
                }
            }
            if (schema.required.isNotEmpty()) {
                schemaMap["required"] = schema.required
            }
        }

        // For an ARRAY, recursively convert its 'items' schema.
        if (schema.type == DataType.ARRAY && schema.items != null) {
            schemaMap["items"] = convertSchemaToMap(schema.items)
        }

        return schemaMap
    }
}

data class Schema(
    val type: DataType = DataType.UNSPECIFIED,
    val description: String = "",
    val nullable: Boolean = false,
    val enum: List<Any> = emptyList(),
    val items: Schema? = null,
    val properties: Map<String, Schema> = emptyMap(),
    val required: List<String> = emptyList(),
)

enum class DataType {
    UNSPECIFIED,
    BOOLEAN,
    OBJECT,
    DOUBLE,
    FLOAT,
    LONG,
    INT,
    STRING,
    ARRAY,
    UNIT,
}
