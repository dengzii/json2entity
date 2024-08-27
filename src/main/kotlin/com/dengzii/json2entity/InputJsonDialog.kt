package com.dengzii.json2entity

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import json2entity.InputJsonForm
import org.jetbrains.annotations.Nullable
import java.awt.Dimension
import javax.swing.JComponent

class InputJsonDialog(private val e: AnActionEvent) : DialogWrapper(null, false, IdeModalityType.MODELESS) {

    private lateinit var form: InputJsonForm

    init {
        isResizable = false
        title = "Json to Entity"
        init()
    }

    @Nullable
    override fun createCenterPanel(): JComponent {
        form = InputJsonForm()
        isModal = true
        isResizable = true
        form.panel1.preferredSize = Dimension(800, 500)
        return form.panel1
    }

    override fun doOKAction() {
        super.doOKAction()
        val json = form.textAreaJson.text.trim()
        val name = form.textFieldName.text.trim().removeSuffix(".dart")
        if (json.isEmpty()) {
            return
        }
        val nm = name.map {
            if (it.isUpperCase()) {
                return@map "_${it.lowercase()}"
            } else {
                return@map it
            }
        }.joinToString("")
            .removePrefix("_")
            .removeSuffix("_")
            .removeSuffix("bean")

        val j2d = JsonToDart(
            GenerateParam(
                project = e.project!!,
                directory = e.getActionDirectory()!!,
            ), nm, json
        )
        FileWriteCommand.start(e, j2d)
    }
}