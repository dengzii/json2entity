package com.dengzii.json2entity

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.util.ThrowableRunnable

class FileWriteCommand(private val event: AnActionEvent, private val parser: Json2EntityParser) :
    ThrowableRunnable<Exception> {

    companion object {
        fun start(event: AnActionEvent, parser: Json2EntityParser) {
            WriteCommandAction.writeCommandAction(event.project)
                .withName("Json to Entity")
                .withGlobalUndo()
                .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
                .run(FileWriteCommand(event, parser))
        }
    }

    override fun run() {
        parser.parseJson()
        parser.generateTypes()
    }
}