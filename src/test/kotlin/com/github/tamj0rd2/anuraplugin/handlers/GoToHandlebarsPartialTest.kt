package com.github.tamj0rd2.anuraplugin.handlers

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase

class GoToHandlebarsPartialTest : BasePlatformTestCase() {
    fun `test going to a referenced partial`() {
        val files = mapOf(
            "Partial.hbs" to
                // language=Handlebars
                """
                |<h1>Hello world</h1>
                """.trimMargin(),
            "View.hbs" to
                """
                |{{>src/<caret>Partial}}
                """.trimMargin(),
        )
        files.forEach { (fileName, content) -> myFixture.configureByText(fileName, content) }

        val targetElements = GotoDeclarationAction.findAllTargetElements(project, myFixture.editor, myFixture.caretOffset)

        TestCase.assertEquals(1, targetElements.size)
    }
}