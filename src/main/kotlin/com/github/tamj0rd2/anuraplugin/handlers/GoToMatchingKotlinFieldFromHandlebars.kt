package com.github.tamj0rd2.anuraplugin.handlers

import com.dmarcotte.handlebars.psi.HbMustacheName
import com.dmarcotte.handlebars.psi.HbParam
import com.dmarcotte.handlebars.psi.HbPsiElement
import com.dmarcotte.handlebars.psi.HbSimpleMustache
import com.github.tamj0rd2.anuraplugin.handlers.HbsUtils.isHbsIdElement
import com.github.tamj0rd2.anuraplugin.services.MyProjectService
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.parents

internal class GoToMatchingKotlinFieldFromHandlebars : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(element: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        if (element == null) return null
        if (!element.isHbsIdElement()) return null
        val service = editor?.project?.service<MyProjectService>() ?: return null

        val canGoToDeclaration = element.parents.any { it is HbSimpleMustache || it is HbParam }
        if (!canGoToDeclaration) return null

        val fullHandlebarsVariable = element.parentOfType<HbMustacheName>() ?: return emptyArray()
        val identifierParts = getHbsIdentifierParts(fullHandlebarsVariable, element)

        return service.findKotlinReferences(
            hbsFile = element.containingFile.virtualFile,
            hbsIdentifierParts = identifierParts,
        ).toTypedArray()
    }

    private fun getHbsIdentifierParts(
        fullHandlebarsVariable: HbMustacheName,
        element: PsiElement,
    ): List<String> {
        var foundThisElement = false
        return fullHandlebarsVariable.collectDescendantsOfType<HbPsiElement> { descendent ->
            if (foundThisElement) return@collectDescendantsOfType false
            if (!descendent.isHbsIdElement()) return@collectDescendantsOfType false
            if (descendent == element.context) foundThisElement = true
            true
        }.map { it.text }
    }
}
