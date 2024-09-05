package com.dengzii.json2entity

import com.dengzii.json2entity.Json2EntityParser.Companion.log
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.DartFile

interface CodeGenerator {
    fun generate(fileName: String, code: String)
}

class DartCodeGenerator(private val project: Project, private val directory: PsiDirectory) : CodeGenerator {
    private val fileFactory by lazy { PsiFileFactory.getInstance(project) }
    private val documentManager by lazy { PsiDocumentManager.getInstance(project) }
    private val codeStyle by lazy { CodeStyleManager.getInstance(project) }
    private val language = DartLanguage.INSTANCE

    override fun generate(fileName: String, code: String) {
        if (directory.findFile(fileName) != null) {
            return
        }

        val dartFile = fileFactory.createFileFromText(fileName, language, code) as DartFile

        codeStyle.reformat(dartFile)

        directory.add(dartFile)

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
}