package com.github.tamj0rd2.anuraplugin.services

import com.dmarcotte.handlebars.parsing.HbTokenTypes
import com.dmarcotte.handlebars.psi.HbBlockWrapper
import com.dmarcotte.handlebars.psi.HbOpenBlockMustache
import com.dmarcotte.handlebars.psi.HbParam
import com.dmarcotte.handlebars.psi.HbSimpleMustache
import com.github.tamj0rd2.anuraplugin.handlers.HbsUtils.isHbsIdElement
import com.intellij.openapi.components.Service
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.util.takeWhileInclusive
import org.toml.lang.psi.ext.elementType

interface IHbsService {
    fun getHbsIdentifierParts(element: PsiElement): List<String>
}

@Service(Service.Level.PROJECT)
class HbsService : IHbsService {

    override fun getHbsIdentifierParts(element: PsiElement): List<String> {
        if (!element.isHbsIdElement()) return emptyList()

        return element.parentsWithSelf
            .fold(emptyList(), ::foldHbsIdentifierParts)
            .takeWhileInclusive { it != element.text }
    }

    fun isElementAllowedToBeSearchedFor(element: PsiElement) =
        element.parents.any {
            when(it) {
                is HbSimpleMustache,
                is HbParam -> true

                else -> false
            }
        }

    private fun foldHbsIdentifierParts(identifierParts: List<String>, element: PsiElement): List<String> {
        when (element) {
            is HbSimpleMustache -> {
                val newParts = element.name?.split(".") ?: emptyList()
                return newParts + identifierParts
            }

            is HbParam -> {
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
