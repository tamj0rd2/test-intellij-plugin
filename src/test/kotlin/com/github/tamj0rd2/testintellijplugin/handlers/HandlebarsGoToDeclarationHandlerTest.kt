package com.github.tamj0rd2.testintellijplugin.handlers

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class HandlebarsGoToDeclarationHandlerTest : BasePlatformTestCase() {
    fun `test going to declaration of variable that is a kotlin field`() {
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

    fun `test going to declaration of variable that is a kotlin property`() {
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

    fun `test going to declaration of variable that includes nesting`() {
        runGoToDeclarationTest(
            kotlinFilesContent = mapOf(
                "ViewModel.kt" to
                        // language=Kt
                        """
                        |package models
                        |data class ViewModel(val person: Person)
                        """.trimMargin(),
                "Person.kt" to
                        // language=Kt
                        """
                        |package models
                        |data class Person(val name: String)
                        """.trimMargin(),
            ),
            handlebarsFileContent = "<h1>{{<caret>person.name}}, world</h1>",
            expectedReferences = listOf(
                ExpectedReference(
                    name = "person",
                    definedBy = "ViewModel"
                )
            )
        )
    }

    fun `test going to declaration of variable that includes nesting - nested`() {
        runGoToDeclarationTest(
            kotlinFilesContent = mapOf(
                "ViewModel.kt" to
                        // language=Kt
                        """
                        |package models
                        |data class ViewModel(val person: Person)
                        """.trimMargin(),
                "Person.kt" to
                        // language=Kt
                        """
                        |package models
                        |data class Person(val name: String)
                        """.trimMargin(),
            ),
            handlebarsFileContent = "<h1>{{person.<caret>name}}, world</h1>",
            expectedReferences = listOf(
                ExpectedReference(
                    name = "name",
                    definedBy = "Person"
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
    ) = runGoToDeclarationTest(
        kotlinFilesContent = mapOf("ViewModel.kt" to kotlinFileContent),
        handlebarsFileContent = handlebarsFileContent,
        expectedReferences = expectedReferences,
    )

    private fun runGoToDeclarationTest(
        kotlinFilesContent: Map<String, String>,
        handlebarsFileContent: String,
        expectedReferences: List<ExpectedReference>,
    ) {
        kotlinFilesContent.forEach { (fileName, content) -> myFixture.configureByText(fileName, content) }
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