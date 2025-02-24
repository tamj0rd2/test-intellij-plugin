package com.github.tamj0rd2.anuraplugin.handlers

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

object HbsUtils {
    fun PsiElement.isHbsIdElement(): Boolean {
        // the Handlebars PSI doesn't seem to expose any other way to check this.
        @Suppress("UnstableApiUsage")
        return this.elementType?.debugName == "ID"
    }
}