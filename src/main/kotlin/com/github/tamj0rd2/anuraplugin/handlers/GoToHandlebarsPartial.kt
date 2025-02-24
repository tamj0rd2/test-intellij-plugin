package com.github.tamj0rd2.anuraplugin.handlers

import com.dmarcotte.handlebars.psi.HbPartial
import com.dmarcotte.handlebars.psi.impl.HbPathImpl
import com.github.tamj0rd2.anuraplugin.handlers.HbsUtils.isHbsIdElement
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType

internal class GoToHandlebarsPartial : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(element: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        if (element == null) return null
        if (!element.isHbsIdElement()) return null

        val partial = element.parentOfType<HbPartial>(true) ?: return null

        // only make the actual file name navigable.
        if (element.parentOfType<HbPathImpl>()?.lastChild != element.context) return null

        val partialName = partial.name.substringAfterLast("/")
        val matchingFiles = FilenameIndex.getVirtualFilesByName("$partialName.hbs", GlobalSearchScope.allScope(partial.project))
        return matchingFiles.mapNotNull { it.findPsiFile(partial.project) }.toTypedArray()
    }
}
