package com.dengzii.json2entity


class JsonToDart(
    private val codeGenerator: CodeGenerator, private val param: GenerateParam, name: String, input: String
) : Json2EntityParser(name, input, param) {

    private val nullable = param.nullable
    private val dynamic = TypeRefer("dynamic", "dynamic", "", "")

    private val primitiveTypeRefers = mapOf(
        JsonType.STRING.uniqueId to TypeRefer("String", "String", "", ""),
        JsonType.INT.uniqueId to TypeRefer("int", "int", "", ""),
        JsonType.BOOL.uniqueId to TypeRefer("bool", "bool", "", ""),
        JsonType.FLOAT.uniqueId to TypeRefer("num", "num", "", ""),
        JsonType.DOUBLE.uniqueId to TypeRefer("num", "num", "", ""),
    )

    private val emptyContainer = mapOf(
        JsonType.EMPTY_ARRAY.uniqueId to TypeRefer("dynamic", "dynamic", "", "", array = true),
        JsonType.EMPTY_OBJ.uniqueId to dynamic,
        JsonType.UNKNOWN.uniqueId to dynamic,
    )

    private val primitiveRefers = primitiveTypeRefers.values + dynamic
    private val naming = DefaultNaming(suffix = param.suffix)

    override fun getTypeRefer(type: JsonType): TypeRefer {
        if (emptyContainer.containsKey(type.uniqueId)) {
            return emptyContainer[type.uniqueId]!!
        }
        return super.getTypeRefer(type)
    }

    override fun getDefaultTypeRefers(): Map<String, TypeRefer> = primitiveTypeRefers

    override fun generateType(type: JsonType, name: String, fields: Map<String, TypeRefer>): TypeRefer {
        val entityName = naming.nameEntity(name)
        val fileName = naming.nameFile(name, "dart")
        generateFile(type, fileName, entityName, fields)
        log("gen type => $fileName, $entityName")
        return TypeRefer(entityName, fileName, "", "")
    }

    private fun generateFile(type: JsonType, fileName: String, entityName: String, jsonKeys: Map<String, TypeRefer>) {

        val sortedJsonKeys = jsonKeys.map {
            it.key to it.value
        }.sortedBy {
            when (param.sort) {
                FieldSort.Nullable, FieldSort.Type, FieldSort.TypeAndNullable -> when {
                    it.second in primitiveRefers -> -1
                    it.second.array -> 0
                    else -> 1
                }
            }
        }

        val imports =
            sortedJsonKeys.asSequence().map { it.second }.distinctBy { it.name }.map { it.copy(array = false) }
                .filter { it !in primitiveRefers }.map { "import '${it.reference}';" }.joinToString("\n")

        val jsonId = if (param.jsonId) "@pragma(\"json_id:${type.uniqueId}\")" else ""
        val fields = sortedJsonKeys.joinToString("\n\t") { genFieldDeclare(it.first, it.second) }
        val constructorParams = sortedJsonKeys.joinToString(",\n\t\t") { (key, t) -> genConstructParam(key, t) }

        val filterNull = if (param.toJsonSkipNullKey) "d\n\t\tfata.removeWhere((k, v) => v == null);" else ""

        val toJson = if (param.genToJson) {
            """
    Map<String, dynamic> toJson() {
        final Map<String, dynamic> data = <String, dynamic>{};
        ${sortedJsonKeys.joinToString("\n\t\t") { genFieldToJsonPutMap("data", it.first, it.second) }} $filterNull
        return data;
    }
"""
        } else {
            ""
        }

        val code = """
$imports

$jsonId
class $entityName {
    $fields
    
    $entityName({
        $constructorParams,
    });
    
    factory $entityName.fromJson(dynamic data) {
        final json = data as Map<String, dynamic>;
        return $entityName(
            ${sortedJsonKeys.joinToString(",\n\t\t\t") { (k, t) -> genFieldFromJson("json", k, t) }},
        );
    }
    $toJson
}
"""
        codeGenerator.generate(fileName, code)
    }

    private fun genConstructParam(key: String, refer: TypeRefer): String {
        val name = naming.nameField(key)
        val isPrimitiveOrArray = refer in primitiveRefers || refer.array
        val t = if (isPrimitiveOrArray && !nullable) {
            "required this.$name"
        } else {
            "this.$name"
        }
        return t
    }

    private fun genFieldFromJson(map: String, key: String, refer: TypeRefer): String {
        var nullStat = ""
        val value = if (refer !in primitiveRefers) {
            if (refer.array) {
                val cast = " as Iterable?"
                nullStat = " ?? []"
                if (refer.copy(array = false) in primitiveRefers) {
                    val cast1 = if (refer == dynamic) "" else " as ${refer.reference}"
                    "($map['$key']$cast)?.map((e) => e${cast1}).toList()"
                } else {
                    "($map['$key']$cast)?.map((e) => ${refer.name}.fromJson(e)).toList()"
                }
            } else {
                "$map['$key'] != null ? ${refer.name}.fromJson(json['$key']) : null"
            }
        } else {
            nullStat = " ?? " + when (refer) {
                primitiveTypeRefers[JsonType.STRING.uniqueId] -> "''"
                primitiveTypeRefers[JsonType.INT.uniqueId] -> "0"
                primitiveTypeRefers[JsonType.BOOL.uniqueId] -> "false"
                primitiveTypeRefers[JsonType.FLOAT.uniqueId] -> "0"
                primitiveTypeRefers[JsonType.DOUBLE.uniqueId] -> "0"
                else -> "null"
            }
            "$map['$key']"
        }

        return "${naming.nameField(key)}: $value$nullStat"
    }

    private fun genFieldToJsonPutMap(map: String, key: String, refer: TypeRefer): String {
        val field = naming.nameField(key)
        val value = if (refer !in primitiveRefers) {
            if (refer.array) {
                val nullable = if (param.nullable) "?" else ""
                if (refer.copy(array = false) in primitiveRefers) {
                    field
                } else {
                    "${field}$nullable.map((e) => e.toJson()).toList()"
                }
            } else {
                "${field}?.toJson()"
            }
        } else {
            field
        }
        return "$map['$key'] = $value;"
    }

    private fun genFieldDeclare(key: String, refer: TypeRefer): String {
        val name = naming.nameField(key)
        var type = if (refer in primitiveRefers || !refer.array) {
            refer.name
        } else {
            "List<${refer.name}>"
        }
        if (nullable && refer != dynamic) {
            type = "$type?"
        }
        return "final $type ${name};"
    }

    companion object {
        @JvmStatic
        fun main(arg: Array<String>) {
            val jsonToDart = JsonToDart(
                object : CodeGenerator {
                    override fun generate(fileName: String, code: String) {
                        log("generate => $fileName")
                        log(code)
                    }
                }, GenerateParam(
                    nullable = false, genToJson = true, toJsonSkipNullKey = true, suffix = "bean", jsonId = true
                ), name = "hello_world", input = json2
            )
            jsonToDart.parseJson()
            jsonToDart.generateTypes()
        }
    }
}