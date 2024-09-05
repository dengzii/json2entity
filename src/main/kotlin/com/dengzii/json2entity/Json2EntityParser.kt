package com.dengzii.json2entity

import kotlinx.serialization.json.*


data class JsonType(
    val uniqueId: String,
    val fields: Map<String, JsonType>?,
    val depth: Int = 0,
    val array: Boolean = false,
) {

    val isPrimitive get() = this in primitives
    val isContainer get() = uniqueId.startsWith("array") || uniqueId.startsWith("map")

    companion object {

        val STRING = JsonType("string", null)
        val BOOL = JsonType("bool", null)
        val INT = JsonType("int", null)
        val FLOAT = JsonType("float", null)
        val DOUBLE = JsonType("double", null)
        val UNKNOWN = JsonType("unknown", null)
        val EMPTY_ARRAY = JsonType("list_empty", null)
        val EMPTY_OBJ = JsonType("map_empty", null)

        val primitives = setOf(STRING, BOOL, INT, FLOAT, DOUBLE)

        fun array(typeId: String): JsonType {
            return JsonType(typeId, null, array = true)
        }
    }

    fun hasNestedType(): Boolean {
        return fields != null && fields.any { !it.value.isPrimitive && it.value != UNKNOWN && it.value != EMPTY_OBJ && it.value != EMPTY_ARRAY && !it.value.isContainer }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as JsonType?
        return uniqueId == that?.uniqueId
    }

    override fun hashCode(): Int {
        return uniqueId.hashCode()
    }

    override fun toString(): String {
        if (isPrimitive) {
            return uniqueId
        }
        return "JsonType(uniqueId=$uniqueId, fields=$fields)"
    }
}

data class TypeRefer(
    val name: String,
    val reference: String,
    val path: String,
    val filename: String,
    val array: Boolean = false,
    val builtIn: Boolean = false,
) {
    override fun toString(): String {
        return if (array) "List<$name>" else name
    }
}

enum class FieldSort {
    Nullable,
    Type,
    TypeAndNullable,
}

class GenerateParam(
    val nullable: Boolean,
    val genToJson: Boolean,
    val toJsonSkipNullKey: Boolean,
    val jsonId: Boolean,
    val suffix: String,
    val sort: FieldSort = FieldSort.TypeAndNullable,
)

abstract class Json2EntityParser(private val name: String, private val input: String, param: GenerateParam?) {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val md5 = java.security.MessageDigest.getInstance("MD5")
    private val types = HashSet<JsonType>()
    private val node2type = HashMap<String, String>()
    private val type2ref = HashMap<String, TypeRefer>()
    private val type2node = HashMap<String, MutableList<String>>()

    private val typeNames = HashSet<String>()

    private lateinit var json: JsonElement

    abstract fun getDefaultTypeRefers(): Map<String, TypeRefer>

    open fun generateType(type: JsonType, name: String, fields: Map<String, TypeRefer>): TypeRefer {
        log("gen type => $name, $fields")
        return TypeRefer(name = name, reference = name, path = "", filename = "")
    }

    open fun getTypeRefer(type: JsonType): TypeRefer {
        val ref = type2ref[type.uniqueId]?.copy(array = type.array)
            ?: throw IllegalStateException("type ref is null, type => $type, node => ${type2node[type.uniqueId]?.firstOrNull()}")
        return ref
    }

    open fun getTypeName(type: JsonType): String {
        val node = type2node[type.uniqueId]?.firstOrNull()
        var name = node?.split(".")?.last() ?: type.uniqueId
        // avoid type name conflict
        while (typeNames.contains(name)) {
            name = "${name}_1"
        }
        typeNames.add(name)
        return name
    }

    open fun filterField(name: String): Boolean {
        return name.isEmpty()
    }

    fun generateTypes() {
        val types = types.filterNot {
            it.isPrimitive || it == JsonType.EMPTY_ARRAY || it == JsonType.UNKNOWN
        }.sortedBy {
            -it.depth
        }
        log("==== start generate types ${types.size} =====")
        for (type in types) {
            log("gen type => ${type.uniqueId}")
            val fields = type.fields!!.filterNot {
                filterField(it.key) || it.value.isContainer
            }.mapValues {
                getTypeRefer(it.value)
            }
            val ref = generateType(type, getTypeName(type), fields)
            type2ref[type.uniqueId] = ref
        }
    }

    fun parseJson() {
        type2ref.putAll(getDefaultTypeRefers())
        json = Json.parseToJsonElement(input)
        json.parseType(name, 0)

        val type2node = HashMap<String, MutableList<String>>()
        this.type2node.putAll(
            type2node.filterNot {
                it.value.size <= 1
            }
        )
        for (key in node2type.keys) {
            log("$key -> ${node2type[key]}")
        }
    }

    open fun identifyType(key2type: Map<String, JsonType>): String {
        val digestSrc = key2type.toSortedMap().map {
            "${it.key}=${it.value.uniqueId}"
        }.joinToString()
        return digest(digestSrc)
    }

    private fun digest(src: String): String {
        val bytes = md5.digest(src.toByteArray())

        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun JsonArray.parseType(node: String, depth: Int): JsonType {
        return if (this.isEmpty()) {
            JsonType.EMPTY_ARRAY
        } else {
            val type = jsonArray[0].parseType(node, depth = depth + 1)
            JsonType.array(type.uniqueId)
        };
    }

    private fun JsonPrimitive.parseType(): JsonType {
        return if (jsonPrimitive.isString) {
            JsonType.STRING
        } else if (jsonPrimitive.booleanOrNull != null) {
            JsonType.BOOL
        } else if (jsonPrimitive.intOrNull != null) {
            JsonType.INT
        } else if (jsonPrimitive.floatOrNull != null) {
            JsonType.FLOAT
        } else if (jsonPrimitive.doubleOrNull != null) {
            JsonType.DOUBLE
        } else {
            JsonType.UNKNOWN
        }
    }

    private fun JsonObject.parseType(node: String, depth: Int): JsonType {
        if (this.isEmpty()) {
            return JsonType.EMPTY_OBJ
        }
        val key2type = HashMap<String, JsonType>()
        jsonObject.entries.forEach { entry ->
            val type = entry.value.parseType("$node.${entry.key}", depth + 1)
            key2type[entry.key] = type
        }
        val id = identifyType(key2type)
        return JsonType(id, key2type, depth = depth)
    }

    private fun JsonElement.parseType(node: String, depth: Int): JsonType {
        val type = when (this) {
            is JsonNull -> JsonType.UNKNOWN
            is JsonArray -> parseType(node, depth)
            is JsonPrimitive -> parseType()
            is JsonObject -> parseType(node, depth)
            else -> JsonType.UNKNOWN
        }
        types.add(type)
        node2type[node] = type.uniqueId
        if (!type.isPrimitive) {
            type2node.getOrPut(type.uniqueId) { mutableListOf() }.add(node)
        }
        return type
    }

    companion object {

        fun log(vararg args: Any) {
            val l = args.joinToString {
                it.toString()
            }
            println(l)
        }
    }
}

const val json2 = """
{
    "f1":{
        "hello": "world"
    },
    "f2": {
        "f1": {
        "hello": "world"
         }
    }
}
"""

const val json1 = """
{
    "name": "dengzii",
    "age": 18.12,
    "ages": 18.121112312313123213412551241231231231261466234,
    "size": 1,
    "address": {
        "province": "guangdong",
        "city": "shenzhen"
    },
    "hobbies": [{
    "name": "1",
    "id": 0
    }],
    "personal_info": {
    "hb": {
    "name": "1",
    "id": 0
    }, 
    "uid": 1
    },
    "": true,
    "addresses": [
    {
        "province": "guangdong",
        "city": "shenzhen"
    },{
        "city": "shenzhen",
        "province": "guangdong"
    }
    ]
}
"""
