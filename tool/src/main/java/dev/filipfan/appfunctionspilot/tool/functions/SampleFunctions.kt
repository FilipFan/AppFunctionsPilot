package dev.filipfan.appfunctionspilot.tool.functions

import android.util.Log
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.service.AppFunction
import org.json.JSONObject
import java.time.LocalDateTime

private const val TAG = "SampleFunctions"

class VoidFunctionImpl : VoidFunction {
    /**
     * A sample function that takes no parameters and returns no value. It is used to demonstrate a simple function invocation.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun voidFunction(appFunctionContext: AppFunctionContext) {
        Log.i(TAG, "voidFunction")
    }
}

class DoThrowImpl : DoThrow {
    /**
     * A sample function that demonstrates how to throw an [AppFunctionInvalidArgumentException]. This
     * exception can be used to indicate that the arguments provided to the function are invalid.
     *
     * @throws AppFunctionInvalidArgumentException whenever this function is called.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun doThrow(appFunctionContext: AppFunctionContext) {
        Log.i(TAG, "doThrow")
        throw AppFunctionInvalidArgumentException("invalid")
    }
}

class DisabledFunctionImpl : DisabledFunction {
    /**
     * A sample function that is disabled. This function will not be invokable by the host.
     */
    @AppFunction(isEnabled = false, isDescribedByKdoc = true)
    override fun disabledFunction(appFunctionContext: AppFunctionContext) {
        Log.i(TAG, "disabledFunction")
    }
}

class FunctionNullableImpl : FunctionNullable {
    /**
     * A sample function that demonstrates the use of nullable types. This function accepts a nullable
     * string and returns a nullable string.
     *
     * @param s The nullable string to be processed.
     * @return "input was null" if the input string is null, otherwise returns null.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun functionNullable(appFunctionContext: AppFunctionContext, s: String?): String? {
        Log.i(TAG, "functionNullable")
        return if (s == null) "input was null" else null
    }
}

class ArgumentOptionalValuesImpl : ArgumentOptionalValues {
    /**
     * A sample function that demonstrates the use of optional values in an enum. This function accepts
     * and returns an enum with optional values.
     *
     * @param v An enum value of [ArgumentOptionalValues.OptionalValues].
     * @return The received enum value.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun argumentOptionalValues(
        appFunctionContext: AppFunctionContext,
        v: ArgumentOptionalValues.OptionalValues,
    ): ArgumentOptionalValues.OptionalValues {
        Log.i(TAG, "argumentOptionalValues")
        return v
    }
}

class AddImpl : Add {
    /**
     * A function that adds two numbers.
     *
     * @param num1 The first number.
     * @param num2 The second number.
     * @return The sum of num1 and num2.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun add(appFunctionContext: AppFunctionContext, num1: Long, num2: Long): Long {
        Log.i(TAG, "add")
        return num1 + num2
    }
}

class GetProductDetailsImpl : GetProductDetails {
    /**
     * Retrieves the details of a product from a static product database.
     *
     * @param productId The ID of the product to retrieve.
     * @return A JSON string containing the product details if found, otherwise a JSON string with an
     * error message.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun getProductDetails(
        appFunctionContext: AppFunctionContext,
        productId: String,
    ): String {
        Log.i(TAG, "getProductDetails")
        data class Product(
            val id: String,
            val name: String,
            val price: Double,
            val stock: Int,
        )

        val productDatabase = listOf(
            Product(id = "p001", name = "Premium Wireless Headphones", price = 199.99, stock = 50),
            Product(id = "p002", name = "Smart Fitness Watch", price = 249.50, stock = 30),
            Product(id = "p003", name = "Mechanical Gaming Keyboard", price = 120.00, stock = 75),
            Product(id = "p004", name = "4K Ultra HD Monitor", price = 450.00, stock = 15),
        )
        val product = productDatabase.find { it.id == productId }

        return if (product != null) {
            JSONObject().apply {
                put("id", product.id)
                put("name", product.name)
                put("price", product.price)
                put("stock", product.stock)
            }.toString()
        } else {
            JSONObject().apply {
                put("error", "Product not found")
                put("productId", productId)
            }.toString()
        }
    }
}

class ProcessProductsImpl : ProcessProducts {
    /**
     * Processes a list of products.
     *
     * @param products A list of ProductInfo objects to be processed.
     * @return A Boolean indicating whether the processing was considered successful.
     * For this example, it simply returns true if the product list was not empty.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun processProducts(
        appFunctionContext: AppFunctionContext,
        products: List<ProcessProducts.ProductInfo>,
    ): Boolean {
        Log.i(TAG, "processProducts called with ${products.size} products.")

        val success = products.isNotEmpty()
        return success
    }
}

class GetLocalDateImpl : GetLocalDate {
    /**
     * Retrieves the current local date and time.
     *
     * @return A [GetLocalDate.DateTime] object representing the current date and time.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun getLocalDate(appFunctionContext: AppFunctionContext): GetLocalDate.DateTime {
        Log.i(TAG, "getLocalDate")
        return GetLocalDate.DateTime(localDateTime = LocalDateTime.now())
    }
}

class GetWeatherImpl : GetWeather {
    /**
     * Retrieves the weather forecast for a given location.
     *
     * @param param The parameters for the weather query, including location and unit.
     * @return A [GetWeather.QueryWeatherResult] object containing the weather information.
     * @throws AppFunctionInvalidArgumentException if the provided unit is not 'celsius' or
     * 'fahrenheit'.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun getWeather(
        appFunctionContext: AppFunctionContext,
        param: GetWeather.QueryWeatherParams,
    ): GetWeather.QueryWeatherResult {
        Log.i(TAG, "getWeather: ${param.additional.info}")
        val normalizedUnit = param.unit.lowercase()
        if (normalizedUnit != "fahrenheit" && normalizedUnit != "celsius") {
            throw AppFunctionInvalidArgumentException("Invalid unit: '${param.unit}'. Please use 'celsius' or 'fahrenheit'.")
        }

        val result = when (param.location.lowercase()) {
            "tokyo" -> GetWeather.QueryWeatherResult(
                temperature = "15",
                unit = normalizedUnit,
                forecast = listOf("sunny", "windy"),
            )

            "san francisco" -> GetWeather.QueryWeatherResult(
                temperature = "72",
                unit = "fahrenheit",
                forecast = listOf("foggy", "drizzling"),
            )

            else -> GetWeather.QueryWeatherResult(
                temperature = "unknown",
                unit = "celsius",
                forecast = listOf(),
            )
        }
        return result
    }
}

class FactoryCreatedFuncAImpl(msg: String) : FactoryCreatedFuncA {
    private val message: String = msg

    /**
     * A sample function that is created by a factory. This function appends a predefined message to the input string.
     *
     * @param raw The input string.
     * @return The input string appended with a message.
     */
    @AppFunction(isDescribedByKdoc = true)
    override fun factoryCreatedFuncA(appFunctionContext: AppFunctionContext, raw: String): String {
        Log.i(TAG, "factoryCreatedFuncA")
        return raw + "-" + this.message
    }
}

class FunctionWithoutSchemaDefinition {
    /**
     * A sample function that does not have a predefined schema definition. This function appends a
     * static string to the input.
     *
     * @param raw The input string.
     * @return The input string appended with a static string.
     */
    @AppFunction(isDescribedByKdoc = true)
    fun functionWithoutSchemaDefinition(
        appFunctionContext: AppFunctionContext,
        raw: String,
    ): String {
        Log.i(TAG, "functionWithoutSchemaDefinition")
        return "$raw-functionWithoutSchemaDefinition"
    }
}
