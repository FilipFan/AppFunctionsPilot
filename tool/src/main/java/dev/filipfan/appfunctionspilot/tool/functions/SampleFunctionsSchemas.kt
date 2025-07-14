package dev.filipfan.appfunctionspilot.tool.functions

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSchemaDefinition
import androidx.appfunctions.AppFunctionSerializable
import java.time.LocalDateTime

// Uses `AppFunctionSchemaDefinition` to define the schema stored in AppSearch when indexing App Functions.
@AppFunctionSchemaDefinition(name = "voidFunction", version = 1, category = "sampleTool")
interface VoidFunction {
    fun voidFunction(appFunctionContext: AppFunctionContext)
}

@AppFunctionSchemaDefinition(name = "doThrow", version = 1, category = "sampleTool")
interface DoThrow {
    fun doThrow(appFunctionContext: AppFunctionContext)
}

@AppFunctionSchemaDefinition(name = "disabledFunction", version = 1, category = "sampleTool")
interface DisabledFunction {
    fun disabledFunction(appFunctionContext: AppFunctionContext)
}

@AppFunctionSchemaDefinition(name = "functionNullable", version = 1, category = "sampleTool")
interface FunctionNullable {
    fun functionNullable(appFunctionContext: AppFunctionContext, s: String?): String?
}

@AppFunctionSchemaDefinition(name = "argumentOptionalValues", version = 1, category = "sampleTool")
interface ArgumentOptionalValues {
    /**
     * A data class demonstrating the use of optional and nullable properties for an App Function.
     *
     * @property optionalNullableInt An optional and nullable integer value.
     * @property optionalNullableLong An optional and nullable long value with a default of 2.
     * @property optionalNullableString An optional and nullable string value.
     */
    @AppFunctionSerializable
    data class OptionalValues(
        val optionalNullableInt: Int?,
        val optionalNullableLong: Long? = 2L,
        val optionalNullableString: String?,
    )

    fun argumentOptionalValues(
        appFunctionContext: AppFunctionContext,
        v: OptionalValues,
    ): OptionalValues
}

@AppFunctionSchemaDefinition(name = "add", version = 1, category = "sampleTool")
interface Add {
    fun add(appFunctionContext: AppFunctionContext, num1: Long, num2: Long): Long
}

@AppFunctionSchemaDefinition(name = "getProductDetails", version = 1, category = "sampleTool")
interface GetProductDetails {
    fun getProductDetails(appFunctionContext: AppFunctionContext, productId: String): String
}

@AppFunctionSchemaDefinition(name = "getLocalDate", version = 1, category = "sampleTool")
interface GetLocalDate {
    /**
     * A data class to hold a [LocalDateTime] object, which is serializable.
     *
     * @property localDateTime The local date and time.
     */
    @AppFunctionSerializable
    data class DateTime(val localDateTime: LocalDateTime) // `LocalDateTime` is serializable.

    fun getLocalDate(appFunctionContext: AppFunctionContext): DateTime
}

@AppFunctionSchemaDefinition(name = "getWeather", version = 1, category = "sampleTool")
interface GetWeather {
    /**
     * Represents the parameters for a weather query.
     *
     * @property location The city for which to get the weather.
     * @property unit The temperature unit, which can be "celsius" or "fahrenheit".
     * @property additional Any additional information for the query.
     */
    @AppFunctionSerializable
    data class QueryWeatherParams(
        val location: String,
        val unit: String,
        val additional: AdditionalInformation,
    )

    /**
     * Represents additional information for a weather query.
     *
     * @property info A string containing extra details for the query.
     */
    @AppFunctionSerializable
    data class AdditionalInformation(
        val info: String,
    )

    /**
     * Holds the result of a weather query.
     *
     * @property temperature The temperature at the specified location.
     * @property unit The unit of temperature, which will be either "celsius" or "fahrenheit".
     * @property forecast A list of strings describing the weather forecast.
     */
    @AppFunctionSerializable
    data class QueryWeatherResult(
        val temperature: String,
        val unit: String,
        val forecast: List<String>,
    )

    fun getWeather(
        appFunctionContext: AppFunctionContext,
        param: QueryWeatherParams,
    ): QueryWeatherResult
}

@AppFunctionSchemaDefinition(name = "factoryCreatedFuncA", version = 1, category = "sampleTool")
interface FactoryCreatedFuncA {
    fun factoryCreatedFuncA(appFunctionContext: AppFunctionContext, raw: String): String
}
