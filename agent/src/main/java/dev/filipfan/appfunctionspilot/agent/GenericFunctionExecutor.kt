package dev.filipfan.appfunctionspilot.agent

import android.annotation.SuppressLint
import android.util.Log
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

class GenericFunctionExecutor(private val manager: AppFunctionManagerCompat) {
    companion object {
        private const val TAG = "GenericFunctionExecutor"
    }

    suspend fun executeAppFunction(
        targetPackageName: String,
        functionDeclaration: FunctionDeclaration,
        arguments: Map<String, JsonElement>,
    ): Result<JsonElement> = Result.runCatching {
        if (!manager.isAppFunctionEnabled(targetPackageName, functionDeclaration.name)) {
            throw IllegalStateException("Function (${functionDeclaration.name}) is disabled")
        }

        val functionParameters = buildAppFunctionData(functionDeclaration.parameters, arguments)
        val request = ExecuteAppFunctionRequest(
            functionIdentifier = functionDeclaration.name,
            targetPackageName = targetPackageName,
            functionParameters = functionParameters,
        )

        when (val response = manager.executeAppFunction(request)) {
            is ExecuteAppFunctionResponse.Success -> parseSuccessResponse(
                functionDeclaration.response,
                response.returnValue,
            )

            is ExecuteAppFunctionResponse.Error -> throw response.error
        }
    }

    @SuppressLint("RestrictedApi")
    private fun buildAppFunctionData(
        schema: Schema?,
        arguments: Map<String, JsonElement>,
    ): AppFunctionData {
        if (schema == null || schema.properties.isEmpty()) return AppFunctionData.EMPTY

        return AppFunctionData.Builder("").apply {
            schema.properties.forEach { (paramName, paramSchema) ->
                val jsonValue = arguments[paramName]
                if (jsonValue != null && jsonValue !is JsonNull) {
                    setValueOnBuilder(this, paramName, paramSchema, jsonValue)
                } else if (schema.required.contains(paramName) && !paramSchema.nullable) {
                    throw IllegalArgumentException("Missing required parameter: $paramName")
                }
            }
        }.build()
    }

    private fun jsonObjectToMap(jsonObject: JsonObject): Map<String, JsonElement> = jsonObject.entrySet().associate { it.key to it.value }

    private fun setValueOnBuilder(
        builder: AppFunctionData.Builder,
        key: String,
        schema: Schema,
        value: JsonElement,
    ) {
        try {
            when (schema.type) {
                DataType.STRING -> builder.setString(key, value.asString)
                DataType.INT -> builder.setInt(key, value.asInt)
                DataType.LONG -> builder.setLong(key, value.asLong)
                DataType.BOOLEAN -> builder.setBoolean(key, value.asBoolean)
                DataType.FLOAT -> builder.setFloat(key, value.asFloat)
                DataType.DOUBLE -> builder.setDouble(key, value.asDouble)
                DataType.OBJECT -> {
                    val subObjectMap = jsonObjectToMap(value.asJsonObject)
                    builder.setAppFunctionData(
                        key,
                        buildAppFunctionData(schema, subObjectMap),
                    )
                }
                DataType.ARRAY -> setArrayValueOnBuilder(builder, key, schema, value.asJsonArray)
                else -> throw IllegalArgumentException("Unsupported data type: ${schema.type} for key '$key'")
            }
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Failed to parse argument '$key' for type ${schema.type}. Reason: ${e.message}",
                e,
            )
        }
    }

    private fun setArrayValueOnBuilder(
        builder: AppFunctionData.Builder,
        key: String,
        schema: Schema,
        value: JsonArray,
    ) {
        val itemsSchema = schema.items
            ?: throw IllegalStateException("Array schema for '$key' is missing 'items' definition.")

        when (itemsSchema.type) {
            DataType.STRING -> builder.setStringList(key, value.map { it.asString })
            DataType.INT -> builder.setIntArray(key, value.map { it.asInt }.toIntArray())
            DataType.LONG -> builder.setLongArray(key, value.map { it.asLong }.toLongArray())
            DataType.OBJECT -> {
                val objectList = value.map {
                    val subObjectMap = jsonObjectToMap(it.asJsonObject)
                    buildAppFunctionData(itemsSchema, subObjectMap)
                }
                builder.setAppFunctionDataList(key, objectList)
            }
            else -> throw IllegalArgumentException("Unsupported array item type: ${itemsSchema.type} for key '$key'")
        }
    }

    private fun parseSuccessResponse(
        responseSchema: Schema?,
        returnValueContainer: AppFunctionData,
    ): JsonElement {
        if (responseSchema == null || responseSchema.type == DataType.UNIT) return JsonNull.INSTANCE

        val returnValueKey = ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
        if (!returnValueContainer.containsKey(returnValueKey)) return JsonNull.INSTANCE

        return getValueFromDataObject(returnValueContainer, returnValueKey, responseSchema)
    }

    private fun convertDataObjectToMap(
        dataObject: AppFunctionData,
        schema: Schema,
    ): JsonObject = JsonObject().apply {
        schema.properties.forEach { (propName, propSchema) ->
            val jsonValue = getValueFromDataObject(dataObject, propName, propSchema)
            if (!jsonValue.isJsonNull) {
                add(propName, jsonValue)
            } else {
                if (propName in schema.required) {
                    Log.w(TAG, "Property $propName $propSchema is missing in the function data")
                }
            }
        }
    }

    private fun getValueFromDataObject(
        data: AppFunctionData,
        key: String,
        schema: Schema,
    ): JsonElement {
        if (!data.containsKey(key)) return JsonNull.INSTANCE

        return when (schema.type) {
            DataType.STRING -> JsonPrimitive(data.getString(key))
            DataType.INT -> JsonPrimitive(data.getInt(key))
            DataType.LONG -> JsonPrimitive(data.getLong(key))
            DataType.BOOLEAN -> JsonPrimitive(data.getBoolean(key))
            DataType.FLOAT -> JsonPrimitive(data.getFloat(key))
            DataType.DOUBLE -> JsonPrimitive(data.getDouble(key))
            DataType.OBJECT -> data.getAppFunctionData(key)
                ?.let { convertDataObjectToMap(it, schema) } ?: JsonNull.INSTANCE
            DataType.ARRAY -> getArrayFromDataObject(data, key, schema) ?: JsonNull.INSTANCE
            else -> throw IllegalArgumentException("Unsupported item type for parsing: ${schema.type}")
        }
    }

    private fun getArrayFromDataObject(
        data: AppFunctionData,
        key: String,
        schema: Schema,
    ): JsonArray? {
        val itemsSchema = schema.items
            ?: throw IllegalStateException("Array schema for '$key' is missing 'items' definition.")

        val elements: List<JsonElement>? = when (itemsSchema.type) {
            DataType.STRING -> data.getStringList(key)?.map { JsonPrimitive(it) }
            DataType.INT -> data.getIntArray(key)?.map { JsonPrimitive(it) }
            DataType.LONG -> data.getLongArray(key)?.map { JsonPrimitive(it) }
            DataType.OBJECT -> data.getAppFunctionDataList(key)?.map {
                convertDataObjectToMap(it, itemsSchema)
            }
            else -> throw IllegalArgumentException("Unsupported array item type for parsing: ${itemsSchema.type}")
        }

        return elements?.let { list ->
            JsonArray().apply { list.forEach(::add) }
        }
    }
}
