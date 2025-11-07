package dev.filipfan.appfunctionspilot.tool.functions

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSchemaDefinition
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionStringValueConstraint
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
     */
    @AppFunctionSerializable(isDescribedByKdoc = true)
    data class OptionalValues(
        /** An optional and nullable integer value. */
        val optionalNullableInt: Int?,
        /** An optional and nullable long value with a default of 2. */
        val optionalNullableLong: Long? = 2L,
        /** An optional and nullable string value. */
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

@AppFunctionSchemaDefinition(name = "processProducts", version = 1, category = "sampleTool")
interface ProcessProducts {
    /**
     * A data class representing product information.
     */
    @AppFunctionSerializable(isDescribedByKdoc = true)
    data class ProductInfo(
        /** The unique SKU (Stock Keeping Unit) for the product. */
        val sku: String,
        /** The number of items in stock. */
        val stockQuantity: Int,
        /** Whether the product is currently active. */
        val isActive: Boolean,
    )

    fun processProducts(appFunctionContext: AppFunctionContext, products: List<ProductInfo>): Boolean
}

@AppFunctionSchemaDefinition(name = "getLocalDate", version = 1, category = "sampleTool")
interface GetLocalDate {
    /**
     * A data class to hold a [LocalDateTime] object, which is serializable.
     */
    @AppFunctionSerializable(isDescribedByKdoc = true)
    data class DateTime(
        /** The local date and time. */
        val localDateTime: LocalDateTime,
    ) // `LocalDateTime` is serializable.

    fun getLocalDate(appFunctionContext: AppFunctionContext): DateTime
}

@AppFunctionSchemaDefinition(name = "getWeather", version = 1, category = "sampleTool")
interface GetWeather {
    /**
     * Represents the parameters for a weather query.
     */
    @AppFunctionSerializable(isDescribedByKdoc = true)
    data class QueryWeatherParams(
        /** The city for which to get the weather. */
        val location: String,
        /** The temperature unit, which can be "celsius" or "fahrenheit". */
        @AppFunctionStringValueConstraint(enumValues = ["celsius", "fahrenheit"])
        val unit: String,
        /** Any additional information for the query. */
        val additional: AdditionalInformation,
    )

    /**
     * Represents additional information for a weather query.
     */
    @AppFunctionSerializable(isDescribedByKdoc = true)
    data class AdditionalInformation(
        /** A string containing extra details for the query. */
        val info: String,
    )

    /**
     * Holds the result of a weather query.
     */
    @AppFunctionSerializable(isDescribedByKdoc = true)
    data class QueryWeatherResult(
        /** The temperature at the specified location. */
        val temperature: String,
        /** The unit of temperature, which will be either "celsius" or "fahrenheit". */
        @AppFunctionStringValueConstraint(enumValues = ["celsius", "fahrenheit"])
        val unit: String,
        /** A list of strings describing the weather forecast. */
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
