package com.dengzii.json2entity

interface Naming {
    fun nameField(name: String): String
    fun nameEntity(name: String): String
    fun nameFile(name: String, extension: String): String
}

class DefaultNaming : Naming {

    private val keywords =
        setOf(
            "num",
            "int",
            "float",
            "double",
            "bool",
            "void",
            "function",
            "null",
            "true",
            "false",
            "return",
            "for",
            "while",
            "if",
            "else",
            "switch",
            "case",
            "default",
            "break",
            "continue",
            "try",
            "catch",
            "finally",
            "throw",
            "class",
            "interface",
            "enum",
            "abstract",
            "final",
        )

    override fun nameField(name: String): String {
        val n = name.trim('_').split('_').joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
            .replaceFirstChar { c -> c.lowercaseChar() }
        return if (keywords.contains(n)) {
            n + "_"
        } else {
            n
        }
    }

    override fun nameEntity(name: String): String {
        return name.trim('_').split('_').joinToString("") { it.replaceFirstChar { c -> c.uppercase() } } + "Bean"
    }

    override fun nameFile(name: String, extension: String): String {
        return name.trim('_') + "_bean.dart"
    }
}