package com.github.tamj0rd2.testintellijplugin

import com.dmarcotte.handlebars.psi.HbPsiFile
import com.github.tamj0rd2.testintellijplugin.services.MyProjectService
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtFile

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun `test validating one to one mapping`() {
        myFixture.configureByFiles("PageView.hbs", "PageViewModel.kt")

        val projectService = project.service<MyProjectService>()
        val result = projectService.validateOneToOneMappingAgainstViewModel(
            assertInstanceOf(
                myFixture.configureByFile("PageView.hbs"),
                HbPsiFile::class.java
            )
        )

        assertEquals(setOf("nonExistentField"), result.fieldsMissingFromViewModel)
    }

    fun `test going to declaration of variable`() {
        myFixture.configureByFiles("PageView.hbs", "PageViewModel.kt")

        val targetElements = GotoDeclarationAction.findAllTargetElements(project, myFixture.editor, 37)
        assertSize(1, targetElements)
    }

    fun `test going to declaration of variable used in if block`() {
        myFixture.configureByText(
            "ViewModel.kt",
            // language=Kt
            "data class ViewModel(val greeting: String?)"
        ) as KtFile

        myFixture.configureByText(
            "View.hbs",
            // language=Handlebars
            "{{#if <caret>greeting}}<h1>Hello world</h1>{{/if}}"
        ) as HbPsiFile

        val targetElements = GotoDeclarationAction.findAllTargetElements(project, myFixture.editor, myFixture.caretOffset)
        assertSize(1, targetElements)
    }

    override fun getTestDataPath() = "src/test/testData/rename"
}
