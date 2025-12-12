package com.dengzii.json2entity

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class Json2EntityAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        InputJsonDialog(e).show()
    }
}
