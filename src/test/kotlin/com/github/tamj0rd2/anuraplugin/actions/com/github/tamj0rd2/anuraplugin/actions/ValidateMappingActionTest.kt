package com.github.tamj0rd2.anuraplugin.actions.com.github.tamj0rd2.anuraplugin.actions

import com.intellij.openapi.actionSystem.Presentation
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.junit.Ignore

class ValidateMappingActionTest : BasePlatformTestCase() {
    @Ignore
    // TODO: figure out how to make this work.
    fun `test validating mapping action`() {
        val output = runAction(
            files = mapOf(
                "ViewModel.kt" to
                        // language=Kt
                        """
                        |data class ViewModel(val greeting: String)
                        """.trimMargin(),
                "View.hbs" to
                        // language=Handlebars
                        "<h1>{{greeting}}, world</h1>",
            ),
        )

        TestCase.assertEquals("what", output.description)
    }

    private fun runAction(files: Map<String, String>): Presentation {
        files.forEach { (fileName, content) -> myFixture.configureByText(fileName, content) }

        return myFixture.testAction(ValidateMappingAction())
    }
}