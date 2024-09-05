package com.dengzii.json2entity

interface Naming {
    fun nameField(name: String): String
    fun nameEntity(name: String): String
    fun nameFile(name: String, extension: String): String
}

class DefaultNaming(private val suffix: String) : Naming {

    private val keywords = setOf(
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

    // filter chars are not allowed in dart
    private val filterCharsReg = Regex("[^a-zA-Z0-9_]")

    override fun nameField(name: String): String {
        val n = filterChars(name).trim('_').split('_')
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
            .replaceFirstChar { c -> c.lowercaseChar() }
        return if (keywords.contains(n)) {
            n + "_"
        } else {
            n
        }
    }

    override fun nameEntity(name: String): String {
        return filterChars(name).trim('_').split('_')
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } } + suffix.lowercase()
            .replaceFirstChar { it.uppercase() }
    }

    override fun nameFile(name: String, extension: String): String {
        return filterChars(name).trim('_') + "_${suffix.lowercase()}.dart"
    }

    private fun filterChars(src: String): String {
        return src.replace(filterCharsReg, "")
    }
}