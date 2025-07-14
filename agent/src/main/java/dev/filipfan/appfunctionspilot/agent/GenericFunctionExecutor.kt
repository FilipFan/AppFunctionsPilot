package dev.filipfan.appfunctionspilot.agent

import android.annotation.SuppressLint
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse

class GenericFunctionExecutor(private val manager: AppFunctionManagerCompat) {

    suspend fun executeAppFunction(
        targetPackageName: String,
        functionDeclaration: FunctionDeclaration,
        arguments: Map<String, Any?>,
    ): Result<Any?> = Result.runCatching {
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
        arguments: Map<String, Any?>,
    ): AppFunctionData {
        if (schema == null || schema.properties.isEmpty()) return AppFunctionData.EMPTY

        return AppFunctionData.Builder("").apply {
            schema.properties.forEach { (paramName, paramSchema) ->
                val value = arguments[paramName]
                if (value != null) {
                    setValueOnBuilder(this, paramName, paramSchema, value)
                } else if (schema.required.contains(paramName) && !paramSchema.nullable) {
                    throw IllegalArgumentException("Missing required parameter: $paramName")
                }
            }
        }.build()
    }

    @Suppress("UNCHECKED_CAST")
    private fun setValueOnBuilder(
        builder: AppFunctionData.Builder,
        key: String,
        schema: Schema,
        value: Any,
    ) {
        when (schema.type) {
            DataType.STRING -> builder.setString(key, value as String)
            DataType.INT -> builder.setInt(key, value as Int)
            DataType.LONG -> builder.setLong(key, value as Long)
            DataType.BOOLEAN -> builder.setBoolean(key, value as Boolean)
            DataType.FLOAT -> builder.setFloat(key, value as Float)
            DataType.DOUBLE -> builder.setDouble(key, value as Double)
            DataType.OBJECT -> {
                require(value is Map<*, *>) { "Value for object schema '$key' must be a Map." }
                builder.setAppFunctionData(
                    key,
                    buildAppFunctionData(schema, value as Map<String, Any?>),
                )
            }

            DataType.ARRAY -> setArrayValueOnBuilder(builder, key, schema, value as List<*>)
            else -> throw IllegalArgumentException("Unsupported data type: ${schema.type} for key '$key'")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setArrayValueOnBuilder(
        builder: AppFunctionData.Builder,
        key: String,
        schema: Schema,
        value: List<*>,
    ) {
        val itemsSchema = schema.items
            ?: throw IllegalStateException("Array schema for '$key' is missing 'items' definition.")

        when (itemsSchema.type) {
            DataType.STRING -> builder.setStringList(key, value as List<String>)
            DataType.INT -> builder.setIntArray(key, (value as List<Int>).toIntArray())
            DataType.LONG -> builder.setLongArray(key, (value as List<Long>).toLongArray())
            DataType.OBJECT -> {
                val objectList = (value as List<Map<String, Any?>>).map {
                    buildAppFunctionData(itemsSchema, it)
                }
                builder.setAppFunctionDataList(key, objectList)
            }

            else -> throw IllegalArgumentException("Unsupported array item type: ${itemsSchema.type} for key '$key'")
        }
    }

    private fun parseSuccessResponse(
        responseSchema: Schema?,
        returnValueContainer: AppFunctionData,
    ): Any? {
        if (responseSchema == null || responseSchema.type == DataType.UNIT) return Unit

        val returnValueKey = ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
        if (!returnValueContainer.containsKey(returnValueKey)) return Unit

        return getValueFromDataObject(returnValueContainer, returnValueKey, responseSchema)
    }

    private fun convertDataObjectToMap(
        dataObject: AppFunctionData,
        schema: Schema,
    ): Map<String, Any?> = schema.properties.mapValues { (propName, propSchema) ->
        getValueFromDataObject(dataObject, propName, propSchema)
    }

    private fun getValueFromDataObject(data: AppFunctionData, key: String, schema: Schema): Any? {
        if (!data.containsKey(key)) return null

        return when (schema.type) {
            DataType.STRING -> data.getString(key)
            DataType.INT -> data.getInt(key)
            DataType.LONG -> data.getLong(key)
            DataType.BOOLEAN -> data.getBoolean(key)
            DataType.FLOAT -> data.getFloat(key)
            DataType.DOUBLE -> data.getDouble(key)
            DataType.OBJECT -> data.getAppFunctionData(key)
                ?.let { convertDataObjectToMap(it, schema) }

            DataType.ARRAY -> getArrayFromDataObject(data, key, schema)
            else -> throw IllegalArgumentException("Unsupported item type for parsing: ${schema.type}")
        }
    }

    private fun getArrayFromDataObject(
        data: AppFunctionData,
        key: String,
        schema: Schema,
    ): List<*>? {
        val itemsSchema = schema.items
            ?: throw IllegalStateException("Array schema for '$key' is missing 'items' definition.")

        return when (itemsSchema.type) {
            DataType.STRING -> data.getStringList(key)
            DataType.INT -> data.getIntArray(key)?.toList()
            DataType.LONG -> data.getLongArray(key)?.toList()
            DataType.OBJECT -> data.getAppFunctionDataList(key)?.map {
                convertDataObjectToMap(it, itemsSchema)
            }

            else -> throw IllegalArgumentException("Unsupported array item type for parsing: ${itemsSchema.type}")
        }
    }
}
