package com.github.tamj0rd2.testintellijplugin.handlers

import com.dmarcotte.handlebars.psi.HbMustacheName
import com.dmarcotte.handlebars.psi.HbPsiElement
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
        if (element == null) return null
        val service = editor?.project?.service<MyProjectService>() ?: return null

        return when {
            element.isHbsIdElement() -> findKotlinReferences(service = service, element = element)
            else -> null
        }
    }

    private fun findKotlinReferences(service: MyProjectService, element: PsiElement): Array<PsiElement> {
        val fullHandlebarsVariable = element.parentOfType<HbMustacheName>() ?: return emptyArray()

        var foundThisElement = false
        val identifierParts = fullHandlebarsVariable.collectDescendantsOfType<HbPsiElement> { descendent ->
            if (foundThisElement) return@collectDescendantsOfType false
            if (!descendent.isHbsIdElement()) return@collectDescendantsOfType false
            if (descendent == element.context) foundThisElement = true
            true
        }.map { it.text }

        return service.findKotlinReferences(
            hbsFile = element.containingFile.virtualFile,
            hbsIdentifierParts = identifierParts,
        ).toTypedArray()
    }

    private fun PsiElement?.isHbsIdElement(): Boolean {
        // the Handlebars PSI doesn't seem to expose any other way to check this.
        @Suppress("UnstableApiUsage")
        return this?.elementType?.debugName == "ID"
    }
}
