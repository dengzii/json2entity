package com.dengzii.json2entity

interface Naming {
    fun nameFiled(name: String): String
    fun nameEntity(name: String): String
    fun nameFile(name: String, extension: String): String
}

class DefaultNaming : Naming {
    override fun nameFiled(name: String): String {
        return name.trim('_').split('_').joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
            .replaceFirstChar { c -> c.uppercase() }
    }

    override fun nameEntity(name: String): String {
        return name.trim('_').split('_').joinToString("") { it.replaceFirstChar { c -> c.uppercase() } } + "Bean"
    }

    override fun nameFile(name: String, extension: String): String {
        return name.trim('_') + "_bean.dart"
    }
}