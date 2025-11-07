package dev.filipfan.appfunctionspilot.agent

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.Type

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MainViewModel"
        private const val TARGET_PACKAGE = "dev.filipfan.appfunctionspilot.tool"
        private const val USE_CONTENT_PROVIDER = false
        private const val AUTHORITY = "dev.filipfan.appfunctionspilot.tool.provider"
        private val CONTENT_URI: Uri = "content://$AUTHORITY".toUri()
        private const val METHOD_GET_METADATA = "get_metadata"
        private const val METHOD_START = "start"
        private const val KEY_METADATA_JSON = "metadata_json"
    }

    private val appFunctionManagerCompat: AppFunctionManagerCompat =
        AppFunctionManagerCompat.getInstance(application)
            ?: throw UnsupportedOperationException("App functions not supported on this device.")

    private val functionExecutor = GenericFunctionExecutor(appFunctionManagerCompat)

    private val gson =
        GsonBuilder()
            .registerTypeAdapter(
                AppFunctionDataTypeMetadata::class.java,
                AppFunctionDataTypeMetadataAdapter(),
            ).create()

    private val contentResolver = application.contentResolver

    private val observer =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                fetchMetadata()
            }
        }

    private val functionMetadataMap =
        MutableStateFlow<Map<FunctionDeclaration, AppFunctionMetadata>>(emptyMap())
    val functionDeclarations: StateFlow<Set<FunctionDeclaration>> =
        functionMetadataMap.map { map -> map.keys }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptySet(),
        )

    private val _functionResponse = MutableStateFlow("Latest function calling response")
    val functionResponse = _functionResponse.asStateFlow()

    init {
        if (USE_CONTENT_PROVIDER) {
            startObservingWithContentProvider()
        } else {
            startObservingWithAppFunctionManager()
        }
    }

    private fun startObservingWithContentProvider() {
        contentResolver.registerContentObserver(CONTENT_URI, true, observer)
        contentResolver.call(AUTHORITY, METHOD_START, null, null)
    }

    private fun stopObservingWithContentProvider() {
        contentResolver.unregisterContentObserver(observer)
    }

    private fun startObservingWithAppFunctionManager() {
        val searchSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_PACKAGE))
        appFunctionManagerCompat
            .observeAppFunctions(searchSpec)
            .onEach { packageList ->
                packageList.firstOrNull()?.let { metadata ->
                    Log.i(
                        TAG,
                        "Received ${metadata.appFunctions.size} functions from $TARGET_PACKAGE",
                    )
                    processPackageMetadata(metadata)
                } ?: run {
                    Log.w(TAG, "Unable to find functions for the target package '$TARGET_PACKAGE'")
                    functionMetadataMap.value = emptyMap()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun getTestArgumentsForFunction(shortName: String): Map<String, Any?> = when (shortName) {
        "functionNullable" -> mapOf("s" to null)
        "argumentOptionalValues" -> mapOf(
            "v" to mapOf(
                "optionalNullableInt" to 123,
                "optionalNullableLong" to 9,
                "optionalNullableString" to null,
            ),
        )

        "add" -> mapOf("num1" to 10L, "num2" to 6L)
        "getProductDetails" -> mapOf("productId" to "p001")
        "processProducts" -> mapOf(
            "products" to listOf(
                mapOf(
                    "sku" to "SKU1001",
                    "stockQuantity" to 50,
                    "isActive" to true,
                ),
                mapOf(
                    "sku" to "SKU1002",
                    "stockQuantity" to 0,
                    "isActive" to false,
                ),
                mapOf(
                    "sku" to "SKU1003",
                    "stockQuantity" to 120,
                    "isActive" to true,
                ),
            ),
        )
        "getWeather" -> mapOf(
            "param" to mapOf(
                "location" to "Tokyo",
                "unit" to "celsius",
                "additional" to mapOf("info" to "ABC"),
            ),
        )

        "factoryCreatedFuncA", "functionWithoutSchemaDefinition" -> mapOf("raw" to "Ping")
        else -> emptyMap()
    }

    fun executeAppFunction(function: FunctionDeclaration) {
        val metadata = functionMetadataMap.value[function]
            ?: throw IllegalStateException("Failed to find AppFunctionMetadata for ${function.shortName}")
        // Test arguments for demonstration.
        Log.i(TAG, "Function invoked: ${function.toJsonString()}")
        val arguments =
            getTestArgumentsForFunction(function.shortName).mapValues { gson.toJsonTree(it.value) }
        viewModelScope.launch {
            val result =
                functionExecutor.executeAppFunction(
                    targetPackageName = TARGET_PACKAGE,
                    appFunctionMetadata = metadata,
                    functionDeclaration = function,
                    arguments = arguments,
                )

            _functionResponse.value = result.fold(
                onSuccess = { it.toString() },
                onFailure = { it.toString() },
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (USE_CONTENT_PROVIDER) {
            stopObservingWithContentProvider()
        }
    }

    private fun fetchMetadata() {
        try {
            val metadataJson = contentResolver.call(AUTHORITY, METHOD_GET_METADATA, null, null)
                ?.getString(KEY_METADATA_JSON)

            if (metadataJson.isNullOrBlank()) {
                Log.w(TAG, "Metadata JSON is null or empty.")
                functionMetadataMap.value = emptyMap()
                return
            }

            val type = object : TypeToken<List<AppFunctionPackageMetadata>>() {}.type
            val packageMetadataList: List<AppFunctionPackageMetadata> =
                gson.fromJson(metadataJson, type)

            packageMetadataList.firstOrNull()?.let { metadata ->
                Log.i(TAG, "Successfully fetched ${metadata.appFunctions.size} metadata items.")
                processPackageMetadata(metadata)
            } ?: run {
                Log.w(TAG, "Unable to find functions via the content provider.")
                functionMetadataMap.value = emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call ToolProvider", e)
            functionMetadataMap.value = emptyMap()
        }
    }

    private fun processPackageMetadata(metadata: AppFunctionPackageMetadata) {
        functionMetadataMap.value = metadata.appFunctions.toFunctionDeclarations()

        viewModelScope.launch {
            val appMetadata = withContext(Dispatchers.IO) {
                metadata.resolveAppFunctionAppMetadata(getApplication())
            }

            val fullDescription = listOfNotNull(
                appMetadata?.description,
                appMetadata?.displayDescription,
            ).joinToString(separator = "\n").trim()

            if (fullDescription.isNotBlank()) {
                // Show the tool app description at first.
                _functionResponse.value = fullDescription
            }
        }
    }
}

class AppFunctionDataTypeMetadataAdapter :
    JsonSerializer<AppFunctionDataTypeMetadata>,
    JsonDeserializer<AppFunctionDataTypeMetadata> {
    companion object {
        private const val CLASSNAME = "DATATYPE_CLASS"
    }

    override fun serialize(
        src: AppFunctionDataTypeMetadata,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        val jsonElement = context.serialize(src)
        if (jsonElement.isJsonObject) {
            jsonElement.asJsonObject.addProperty(CLASSNAME, src.javaClass.name)
        }
        return jsonElement
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): AppFunctionDataTypeMetadata {
        val jsonObject = json.asJsonObject
        val className = jsonObject.get(CLASSNAME).asString
        jsonObject.remove(CLASSNAME)

        try {
            val clazz = Class.forName(className)
            return context.deserialize(jsonObject, clazz)
        } catch (e: ClassNotFoundException) {
            throw JsonParseException(e)
        }
    }
}
