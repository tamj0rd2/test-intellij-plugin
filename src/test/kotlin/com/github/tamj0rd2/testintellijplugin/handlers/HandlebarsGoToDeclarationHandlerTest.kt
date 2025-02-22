package com.github.tamj0rd2.testintellijplugin.handlers

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class HandlebarsGoToDeclarationHandlerTest : BasePlatformTestCase() {
    fun `test going to declaration of variable`() {
        runGoToDeclarationTest(
            // language=Kt
            kotlinFileContent = "data class ViewModel(val greeting: String?)",
            handlebarsFileContent = "<h1>{{<caret>greeting}}, world</h1>",
            expectedReferences = listOf(
                ExpectedReference(
                    name = "greeting",
                    definedBy = "ViewModel"
                )
            )
        )
    }

    fun `test going to declaration of property`() {
        runGoToDeclarationTest(
            // language=Kt
            kotlinFileContent = """class ViewModel { val greeting get() = "Hello" }""",
            handlebarsFileContent = "<h1>{{<caret>greeting}}, world</h1>",
            expectedReferences = listOf(
                ExpectedReference(
                    name = "greeting",
                    definedBy = "ViewModel"
                )
            )
        )
    }

    fun `test going to declaration of variable that includes nesting`() {
        runGoToDeclarationTest(
            // language=Kt
            kotlinFileContent =
                """
                |data class Person(val name: String)
                |data class ViewModel(val person: Person)
                """.trimMargin(),
            handlebarsFileContent = "<h1>{{<caret>person.name}}, world</h1>",
            expectedReferences = listOf(
                ExpectedReference(
                    name = "person",
                    definedBy = "ViewModel"
                )
            )
        )
    }

    fun `test going to declaration of variable used in if block`() {
        runGoToDeclarationTest(
            // language=Kt
            kotlinFileContent = "data class ViewModel(val greeting: String?)",
            handlebarsFileContent = "{{#if <caret>greeting}}<h1>Hello world</h1>{{/if}}",
            expectedReferences = listOf(
                ExpectedReference(
                    name = "greeting",
                    definedBy = "ViewModel"
                )
            )
        )
    }

    private data class ExpectedReference(
        val name: String,
        val definedBy: String,
    )

    private fun runGoToDeclarationTest(
        kotlinFileContent: String,
        handlebarsFileContent: String,
        expectedReferences: List<ExpectedReference>,
    ) {
        myFixture.configureByText("ViewModel.kt", kotlinFileContent)
        myFixture.configureByText("View.hbs", handlebarsFileContent)

        val targetElements =
            GotoDeclarationAction.findAllTargetElements(project, myFixture.editor, myFixture.caretOffset)
                .map {
                    ExpectedReference(
                        name = (it as KtNamedDeclaration).name ?: "undetermined",
                        definedBy = it.getParentOfType<KtClassOrObject>(true)?.name ?: "undetermined"
                    )
                }

        TestCase.assertEquals(expectedReferences, targetElements)
    }
}