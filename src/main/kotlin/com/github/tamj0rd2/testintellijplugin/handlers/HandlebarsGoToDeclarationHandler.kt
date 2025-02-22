package com.github.tamj0rd2.testintellijplugin.handlers

import com.dmarcotte.handlebars.psi.HbMustacheName
import com.dmarcotte.handlebars.psi.HbPsiElement
import com.dmarcotte.handlebars.psi.HbPsiFile
import com.github.tamj0rd2.testintellijplugin.services.MyProjectService
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

internal class HandlebarsGoToDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(element: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        val service = editor?.project?.service<MyProjectService>() ?: return null
        val hbsFile = element?.parentOfType<HbPsiFile>() ?: return null
        val handlebarsVariable = element.parentOfType<HbMustacheName>() ?: return null

        val identifierParts = handlebarsVariable.collectDescendantsOfType<HbPsiElement> { it.elementType?.debugName == "ID" }
            .map { it.text }

        return service.findReferenceInKotlin(hbsFile, identifierParts).toTypedArray()
    }
}
