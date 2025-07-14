package dev.filipfan.appfunctionspilot.agent

data class FunctionDeclaration(
    val name: String,
    val shortName: String,
    val description: String,
    val parameters: Schema? = null,
    val response: Schema? = null,
)

data class Schema(
    val type: DataType = DataType.UNSPECIFIED,
    val description: String = "",
    val nullable: Boolean = false,
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
