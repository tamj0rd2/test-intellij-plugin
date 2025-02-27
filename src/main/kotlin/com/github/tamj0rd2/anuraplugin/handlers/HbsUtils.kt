package com.github.tamj0rd2.anuraplugin.handlers

import com.dmarcotte.handlebars.parsing.HbTokenTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

object HbsUtils {
    fun PsiElement.isHbsIdElement(): Boolean {
        return this.elementType == HbTokenTypes.ID
    }
}