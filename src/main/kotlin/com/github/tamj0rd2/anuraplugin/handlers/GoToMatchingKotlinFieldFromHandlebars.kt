package com.github.tamj0rd2.anuraplugin.handlers

import com.github.tamj0rd2.anuraplugin.handlers.HbsUtils.isHbsIdElement
import com.github.tamj0rd2.anuraplugin.services.HbsService
import com.github.tamj0rd2.anuraplugin.services.MyProjectService
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

internal class GoToMatchingKotlinFieldFromHandlebars : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(element: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        if (element == null) return null
        if (!element.isHbsIdElement()) return null

        val hbsService = editor?.project?.service<HbsService>() ?: return null
        val projectService = editor.project?.service<MyProjectService>() ?: return null

        if (!hbsService.isElementAllowedToBeSearchedFor(element)) return null

        return projectService.findKotlinReferences(
            hbsFile = element.containingFile.virtualFile,
            hbsIdentifierParts = hbsService.getHbsIdentifierParts(element),
        ).toTypedArray()
    }
}
