package com.github.tamj0rd2.anuraplugin.handlers

import com.dmarcotte.handlebars.parsing.HbTokenTypes
import com.dmarcotte.handlebars.psi.HbBlockWrapper
import com.dmarcotte.handlebars.psi.HbOpenBlockMustache
import com.dmarcotte.handlebars.psi.HbParam
import com.dmarcotte.handlebars.psi.HbSimpleMustache
import com.github.tamj0rd2.anuraplugin.handlers.HbsUtils.isHbsIdElement
import com.github.tamj0rd2.anuraplugin.services.MyProjectService
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.util.takeWhileInclusive
import org.toml.lang.psi.ext.elementType

internal class GoToMatchingKotlinFieldFromHandlebars : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(element: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        if (element == null) return null
        if (!element.isHbsIdElement()) return null
        val service = editor?.project?.service<MyProjectService>() ?: return null

        val canGoToDeclaration = element.parents.any { it is HbSimpleMustache || it is HbParam }
        if (!canGoToDeclaration) return null

        val hbsIdentifierParts = element.getHbsIdentifierParts().takeWhileInclusive { it != element.text }
        println(hbsIdentifierParts)
        return service.findKotlinReferences(
            hbsFile = element.containingFile.virtualFile,
            hbsIdentifierParts = hbsIdentifierParts,
        ).toTypedArray()
    }

    private fun PsiElement.getHbsIdentifierParts(): List<String> =
        parentsWithSelf.fold(emptyList()) { identifierParts, element ->
            when (element) {
                is HbSimpleMustache -> {
                    val newParts = element.name?.split(".") ?: emptyList()
                    newParts + identifierParts
                }

                is HbBlockWrapper -> {
                    val openBlock = element.getChildOfType<HbOpenBlockMustache>()!!
                    val param = openBlock.getChildOfType<HbParam>()!!

                    val alias = openBlock.children.firstOrNull { it.elementType == HbTokenTypes.ID }?.text
                    if (alias != null && alias == identifierParts.firstOrNull()) {
                        val updatedParts = listOf(param.text) + "this" + identifierParts.drop(1)
                        return@fold updatedParts
                    }

                    val newParts = param.text?.split(".") ?: emptyList()
                    newParts + identifierParts
                }

                else -> identifierParts
            }
        }
}
