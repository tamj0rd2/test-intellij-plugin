package com.github.tamj0rd2.anuraplugin.actions.com.github.tamj0rd2.anuraplugin.actions

import com.dmarcotte.handlebars.psi.HbPsiFile
import com.github.tamj0rd2.anuraplugin.services.MyProjectService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.findPsiFile
import org.jetbrains.kotlin.idea.refactoring.hostEditor


class ValidateMappingAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val hbsFile = e.dataContext.hostEditor?.virtualFile?.findPsiFile(project) as? HbPsiFile ?: return
        val service = project.service<MyProjectService>()

        val result = service.validateOneToOneMappingAgainstViewModel(hbsFile)

        val message =
            """
            |${result.viewModelName} is missing fields required by ${hbsFile.virtualFile.name}:
            |
            |${result.fieldsMissingFromViewModel.joinToString("\n")}
            """.trimMargin()

        Messages.showMessageDialog(
            project,
            message,
            "Validation Result",
            Messages.getInformationIcon()
        )
    }
}
