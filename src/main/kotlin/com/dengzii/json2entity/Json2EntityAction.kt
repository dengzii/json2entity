package com.dengzii.json2entity

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.writeText
import kotlin.jvm.Throws

class Json2EntityAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        InputJsonDialog(e).show()
    }
}
