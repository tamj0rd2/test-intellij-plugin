package com.github.tamj0rd2.testintellijplugin.handlers

import com.dmarcotte.handlebars.psi.HbSimpleMustache
import com.github.tamj0rd2.testintellijplugin.services.MyProjectService
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType

internal class HandlebarsGoToDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(element: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        val service = editor?.project?.service<MyProjectService>() ?: return null
        val handlebarsVariable = element?.parentOfType<HbSimpleMustache>() ?: return null
        return service.findReferenceInKotlin(handlebarsVariable).toTypedArray()
    }
}