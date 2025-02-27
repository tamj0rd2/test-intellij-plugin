package com.github.tamj0rd2.anuraplugin.annotators

import com.github.tamj0rd2.anuraplugin.handlers.HbsUtils.isHbsIdElement
import com.github.tamj0rd2.anuraplugin.services.HbsService
import com.github.tamj0rd2.anuraplugin.services.MyProjectService
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement

internal class MyAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val hbsService = element.project.service<HbsService>()
        val projectService = element.project.service<MyProjectService>()

        if (!element.isHbsIdElement()) return
        if (!hbsService.isElementAllowedToBeSearchedFor(element)) return

        val definitions = projectService.findKotlinReferences(
            hbsFile = element.containingFile.virtualFile,
            hbsIdentifierParts = hbsService.getHbsIdentifierParts(element)
        )

        if (definitions.isNotEmpty()) return

        holder.newSilentAnnotation(HighlightSeverity.ERROR)
            .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
            .create()
    }
}
