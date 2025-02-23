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
        val hbsFile = element?.containingFile as? HbPsiFile ?: return null
        if (!element.isHbsIdElement()) return null

        val fullHandlebarsVariable = element.parentOfType<HbMustacheName>() ?: return null

        var foundThisElement = false
        val identifierParts = fullHandlebarsVariable.collectDescendantsOfType<HbPsiElement> { descendent ->
            if (foundThisElement) return@collectDescendantsOfType false
            if (!descendent.isHbsIdElement()) return@collectDescendantsOfType false
            if (descendent == element.context) foundThisElement = true
            true
        }.map { it.text }

        return service.findKotlinReferences(
            hbsFile = hbsFile.virtualFile,
            hbsIdentifierParts = identifierParts,
        ).toTypedArray()
    }

    private fun PsiElement?.isHbsIdElement(): Boolean {
        // the Handlebars PSI doesn't seem to expose any other way to check this.
        @Suppress("UnstableApiUsage")
        return this?.elementType?.debugName == "ID"
    }
}
