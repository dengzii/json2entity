package com.dengzii.json2entity

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory


fun AnActionEvent.getActionDirectory(): PsiDirectory? {
    val e = getData(PlatformDataKeys.PSI_ELEMENT);
    if (e is PsiDirectory) {
        return e
    }
    return null
}

fun AnActionEvent.isProjectValid(): Boolean {
    return project?.isOpen == true && project?.isInitialized == true
}

fun AnActionEvent.getActionVirtualFile(): VirtualFile? {
    return getData(PlatformDataKeys.VIRTUAL_FILE)
}

fun AnActionEvent.createDirectoryOnAction(name: String): VirtualFile? {
    val vf = getActionVirtualFile()
    if (vf == null || !vf.isDirectory) {
        throw Exception("target is null or is not a directory.")
    }
    if (vf.findChild(name)?.exists() == true) {
        return vf.findChild(name)
    }
    return vf.createChildDirectory(this, name)
}


fun AnActionEvent.createFileOnAction(name: String): VirtualFile? {
    val vf = getActionVirtualFile()
    if (!checkCreateFile(name, vf)) {
        return null
    }
    return try {
        vf!!.createChildData(this, name)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun checkCreateFile(name: String, vf: VirtualFile?): Boolean {
    if (vf == null || !vf.isDirectory) {
        return false
    }
    if (vf.findChild(name)?.exists() == true) {
        return false
    }
    return true
}