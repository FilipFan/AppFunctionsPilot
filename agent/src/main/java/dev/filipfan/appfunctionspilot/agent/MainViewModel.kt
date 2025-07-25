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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.lang.reflect.Type

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MainViewModel"
        private const val TARGET_PACKAGE = "dev.filipfan.appfunctionspilot.tool"
        private const val USE_CONTENT_PROVIDER = true
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

    private val _functionDeclarations = MutableStateFlow<List<FunctionDeclaration>>(emptyList())
    val functionDeclarations: StateFlow<List<FunctionDeclaration>> =
        _functionDeclarations.asStateFlow()

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
        val searchFunctionSpec =
            AppFunctionSearchSpec(packageNames = setOf(TARGET_PACKAGE))
        appFunctionManagerCompat
            .observeAppFunctions(searchFunctionSpec)
            .onEach { metadataList ->
                Log.i(TAG, "Received functions: ${metadataList.size}")
                _functionDeclarations.value = metadataList.toFunctionDeclarations()
            }.launchIn(viewModelScope)
    }

    private fun getTestArgumentsForFunction(shortName: String): Map<String, Any?> = when (shortName) {
        "functionNullable" -> mapOf("s" to null)
        "argumentOptionalValues" -> mapOf(
            "v" to mapOf(
                "optionalNullableInt" to 123,
                "optionalNullableLong" to null,
                "optionalNullableString" to "test",
            ),
        )

        "add" -> mapOf("num1" to 10L, "num2" to 6L)
        "getProductDetails" -> mapOf("productId" to "p001")
        "getWeather" -> mapOf(
            "param" to mapOf(
                "location" to "Tokyo",
                "unit" to "celsius",
                "additional" to mapOf("info" to "ABC"),
            ),
        )

        "factoryCreatedFuncA" -> mapOf("raw" to "Ping")
        else -> emptyMap()
    }

    fun executeAppFunction(function: FunctionDeclaration) {
        // Test arguments for demonstration.
        val arguments = getTestArgumentsForFunction(function.shortName)
        viewModelScope.launch {
            val result =
                functionExecutor.executeAppFunction(
                    targetPackageName = TARGET_PACKAGE,
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
            val bundle = contentResolver.call(AUTHORITY, METHOD_GET_METADATA, null, null)
            val metadataJson = bundle?.getString(KEY_METADATA_JSON)

            if (metadataJson.isNullOrEmpty()) {
                Log.w(TAG, "Metadata JSON is null or empty.")
                return
            }

            val type = object : TypeToken<List<AppFunctionMetadata>>() {}.type
            val metadataList: List<AppFunctionMetadata> = gson.fromJson(metadataJson, type)

            Log.i(TAG, "Successfully fetched ${metadataList.size} metadata items.")
            _functionDeclarations.value = metadataList.toFunctionDeclarations()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call ToolProvider", e)
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
