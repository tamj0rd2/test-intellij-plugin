package com.github.tamj0rd2.anuraplugin.handlers

import com.dmarcotte.handlebars.parsing.HbTokenTypes
import com.dmarcotte.handlebars.psi.HbBlockWrapper
import com.dmarcotte.handlebars.psi.HbOpenBlockMustache
import com.dmarcotte.handlebars.psi.HbParam
import com.dmarcotte.handlebars.psi.HbSimpleMustache
import com.dmarcotte.handlebars.psi.impl.HbParamImpl
import com.github.tamj0rd2.anuraplugin.handlers.HbsUtils.isHbsIdElement
import com.github.tamj0rd2.anuraplugin.services.MyProjectService
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
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

        val hbsIdentifierParts = element.parentsWithSelf
            .fold(emptyList(), ::getHbsIdentifierParts)
            .takeWhileInclusive { it != element.text }

        thisLogger().info("tam service: hbs identifiers are: $hbsIdentifierParts")

        return service.findKotlinReferences(
            hbsFile = element.containingFile.virtualFile,
            hbsIdentifierParts = hbsIdentifierParts,
        ).toTypedArray()
    }

    private fun getHbsIdentifierParts(identifierParts: List<String>, element: PsiElement): List<String> {
        when (element) {
            is HbSimpleMustache -> {
                val newParts = element.name?.split(".") ?: emptyList()
                return newParts + identifierParts
            }

            is HbParamImpl -> {
                val newParts = element.text?.split(".") ?: emptyList()
                return newParts + identifierParts
            }

            is HbBlockWrapper -> {
                val openBlock = element.getChildOfType<HbOpenBlockMustache>() ?: return identifierParts
                if (openBlock.name == "if") return identifierParts

                val param = openBlock.getChildOfType<HbParam>() ?: return identifierParts

                val alias = openBlock.children.firstOrNull { it.elementType == HbTokenTypes.ID }?.text
                if (alias != null && alias == identifierParts.firstOrNull()) {
                    val updatedParts = listOf(param.text) + "this" + identifierParts.drop(1)
                    return updatedParts
                }

                val newParts = param.text?.split(".") ?: emptyList()
                return newParts + identifierParts
            }

            else -> return identifierParts
        }
    }
}
