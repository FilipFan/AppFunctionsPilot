package dev.filipfan.appfunctionspilot.tool

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.core.net.toUri
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicReference

/**
 * This class queries AppFunctionMetadata via AppFunctionManagerCompat and provides the
 * results to the agent.
 *
 * Note: This query operation should be executable directly on the agent side.
 * However, in my test environment, it was found that query results could only be
 * successfully obtained within its own application, hence this indirect method is adopted.
 *
 * This class should be removed when the agent can directly obtain results via AppFunctionManagerCompat#observeAppFunctions.
 */
class ToolProvider : ContentProvider() {

    companion object {
        private const val TAG = "ToolProvider"

        private const val AUTHORITY = "dev.filipfan.appfunctionspilot.tool.provider"
        private val CONTENT_URI: Uri = "content://$AUTHORITY".toUri()

        private const val METHOD_GET_METADATA = "get_metadata"
        private const val METHOD_START = "start"
        private const val KEY_METADATA_JSON = "metadata_json"
    }

    private var metadataItems = AtomicReference<List<AppFunctionMetadata>>(emptyList())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(
                AppFunctionDataTypeMetadata::class.java,
                AppFunctionDataTypeMetadataAdapter(),
            )
            .create()
    }

    private val appFunctionManager by lazy {
        context?.let { AppFunctionManagerCompat.getInstance(it) }
            ?: throw IllegalStateException("Context not available")
    }

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        Log.i(TAG, "Call received with method: $method")
        return when (method) {
            METHOD_GET_METADATA -> {
                handleGetMetadata()
            }

            METHOD_START -> {
                observeAppFunctions()
                null
            }

            else -> throw UnsupportedOperationException("Unknown method: $method")
        }
    }

    private fun handleGetMetadata(): Bundle {
        val metadataJson = gson.toJson(metadataItems.get())
        return Bundle().apply {
            putString(KEY_METADATA_JSON, metadataJson)
        }
    }

    private fun notifyChange() {
        context?.contentResolver?.notifyChange(CONTENT_URI, null)
    }

    private fun observeAppFunctions() {
        val packageName = context?.packageName ?: return
        val searchSpec = AppFunctionSearchSpec(packageNames = setOf(packageName))

        appFunctionManager.observeAppFunctions(searchSpec)
            .onEach { functions ->
                metadataItems.set(functions)
                notifyChange()
            }
            .launchIn(scope)
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int = throw UnsupportedOperationException()

    override fun getType(p0: Uri): String = throw UnsupportedOperationException()

    override fun insert(p0: Uri, p1: ContentValues?): Uri = throw UnsupportedOperationException()

    override fun query(
        p0: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?,
    ): Cursor = throw UnsupportedOperationException()

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int = throw UnsupportedOperationException()
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
