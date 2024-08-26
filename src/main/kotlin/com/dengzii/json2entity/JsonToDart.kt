package com.dengzii.json2entity

import com.intellij.lang.ASTFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PlainTextTokenTypes
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.DartImportStatementImpl
import com.jetbrains.lang.dart.psi.impl.DartVarDeclarationListImpl
import com.jetbrains.lang.dart.util.DartElementGenerator

class JsonToDart(private val param: GenerateParam?, name: String, input: String) :
    Json2EntityParser(name, input, param) {

    private val fileFactory by lazy { PsiFileFactory.getInstance(param!!.project) }
    private val documentManager by lazy { PsiDocumentManager.getInstance(project) }
    private val codeStyle by lazy { CodeStyleManager.getInstance(project) }
    private val language = DartLanguage.INSTANCE

    private val project = param!!.project

    private val primitiveTypeRefers = mapOf(
        JsonType.UNKNOWN.uniqueId to TypeRefer("dynamic", "dynamic", "", "", builtIn = true),
        JsonType.STRING.uniqueId to TypeRefer("String", "String", "", "", builtIn = true),
        JsonType.INT.uniqueId to TypeRefer("int", "int", "", "", builtIn = true),
        JsonType.BOOL.uniqueId to TypeRefer("bool", "bool", "", "", builtIn = true),
        JsonType.FLOAT.uniqueId to TypeRefer("num", "num", "", "", builtIn = true),
        JsonType.DOUBLE.uniqueId to TypeRefer("num", "num", "", "", builtIn = true),
    )

    private val primitiveRefers = primitiveTypeRefers.values

    private val naming = DefaultNaming()

    override fun getDefaultTypeRefers(): Map<String, TypeRefer> = primitiveTypeRefers

    override fun generateType(type: JsonType, name: String, fields: Map<String, TypeRefer>): TypeRefer {
        val entityName = naming.nameEntity(name)
        val fileName = naming.nameFile(name, "dart")
        generateFile(type, fileName, entityName, fields)
        log("gen type => $fileName, $entityName")
        return TypeRefer(entityName, fileName, "", "")
    }

    private fun generateFile(type: JsonType, fileName: String, entityName: String, fields: Map<String, TypeRefer>) {

        val imports = fields.values.toSet().distinctBy { it.name }.filter { it !in primitiveRefers }
        val fieldsNamed = fields.mapKeys {
            naming.nameFiled(it.key)
        }

        val code = """
${imports.joinToString("\n") { "import '${it.reference}';" }}

@pragma("json_id:${type.uniqueId}")
class $entityName {
    ${fieldsNamed.map { fieldDeclare(it.key, it.value) }.joinToString("\n")}
    
    $entityName({
        ${fieldsNamed.map { (key, _) -> "\trequired this.$key" }.joinToString(",\n")},
    });
    
    factory $entityName.fromJson(Map<String, dynamic> json) {
        return $entityName(
            ${fieldsNamed.map { (k, t) -> genFieldFromJson(k, t) }.joinToString(",\n")},
        );
    }
}
"""
        val dartFile = fileFactory.createFileFromText(fileName, language, code) as DartFile

        codeStyle.reformat(dartFile)

        param!!.directory.add(dartFile)

        val fdm = FileDocumentManager.getInstance()
        val document = documentManager.getDocument(dartFile)!!
        fdm.saveDocument(document)

        val vf = fdm.getFile(document)
        if (vf == null) {
            log("file is null")
            return
        }
        documentManager.doPostponedOperationsAndUnblockDocument(document)
//        ReformatCodeProcessor(dartFile, false).run()
        documentManager.commitDocument(document)
    }

    private fun genFieldFromJson(name: String, refer: TypeRefer): String {
        val value = if (refer !in primitiveRefers) {
            if (refer.array) {
                if (refer.copy(array = false) in primitiveRefers) {
                    "json['$name'] != null \n? ${refer.name}.fromJson(json['$name']) \n: null"
                } else {
                    "json['$name'] != null \n? json['$name'].map((e) => ${refer.name}.fromJson(e)).toList() \n: []"
                }
            } else {
                "json['$name'] != null \n? ${refer.name}.fromJson(json['$name']) \n: null"
            }
        } else {
            when (refer) {
                primitiveTypeRefers[JsonType.STRING.uniqueId] -> "json['$name'] ?? ''"
                primitiveTypeRefers[JsonType.INT.uniqueId] -> "json['$name'] ?? 0"
                primitiveTypeRefers[JsonType.BOOL.uniqueId] -> "json['$name'] ?? false"
                primitiveTypeRefers[JsonType.FLOAT.uniqueId] -> "json['$name'] ?? 0.0"
                primitiveTypeRefers[JsonType.DOUBLE.uniqueId] -> "json['$name'] ?? 0.0"
                else -> "json['$name']"
            }
        }
        return "$name: $value"
    }

    private fun fieldDeclare(name: String, refer: TypeRefer): String {
        val type = if (refer in primitiveRefers) {
            refer.name
        } else {
            if (refer.array) {
                "List<${refer.name}>"
            } else {
                "${refer.name}?"
            }
        }
        return "final $type ${name};"
    }

    private fun generateFieldDeclare(refer: TypeRefer, name: String): PsiElement {
        val file = DartElementGenerator.createDummyFile(project, "final ${refer.name} ${name};")
        return file.firstChild
    }

    private fun TypeRefer.toImport(): DartImportStatementImpl? {
        if (this.reference.isBlank() || this in primitiveRefers) return null
        val stat = "import '${this.reference}.dart';"
        val file = DartElementGenerator.createDummyFile(project, stat)
        return file.firstChild as DartImportStatementImpl
    }

    private fun test(file: DartFile) {
        ASTFactory.DefaultFactoryHolder.DEFAULT.createLeaf(
            PlainTextTokenTypes.PLAIN_TEXT,
            "final String name = 'dengzii';"
        )
        DartVarDeclarationListImpl(ASTFactory.whitespace("final String name = 'dengzii';"))
        file.add(DartImportStatementImpl(ASTFactory.whitespace("import 'dart:io';")))
    }
}