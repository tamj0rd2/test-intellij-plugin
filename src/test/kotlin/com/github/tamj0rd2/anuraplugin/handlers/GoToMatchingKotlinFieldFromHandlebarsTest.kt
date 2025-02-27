package com.github.tamj0rd2.anuraplugin.handlers

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class GoToMatchingKotlinFieldFromHandlebarsTest : BasePlatformTestCase() {
    fun `test going to declaration of variable that is a kotlin field`() {
        runGoToKotlinDeclarationTest(
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
        runGoToKotlinDeclarationTest(
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
        runGoToKotlinDeclarationTest(
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


    fun `test going to declaration of variable used in with block`() {
        runGoToKotlinDeclarationTest(
            kotlinFileContent =
                // language=Kt
                """
                |data class Person(val age: String)
                |data class ViewModel(val person: Person)
                """.trimMargin(),
            handlebarsFileContent =
                // language=Handlebars
                """
                |{{#with person}}<h1>{{<caret>age}}</h1>{{/with}}
                """.trimMargin(),
            expectedReferences = listOf(
                ExpectedReference(
                    name = "age",
                    definedBy = "Person"
                )
            )
        )
    }

    fun `test going to declaration of variable used in named with block`() {
        runGoToKotlinDeclarationTest(
            kotlinFileContent =
                // language=Kt
                """
                |data class Person(val age: String)
                |data class ViewModel(val person: Person)
                """.trimMargin(),
            handlebarsFileContent =
                // language=Handlebars
                """
                |{{#with person as |p|}}<h1>{{p.<caret>age}}</h1>{{/with}}
                """.trimMargin(),
            expectedReferences = listOf(
                ExpectedReference(
                    name = "age",
                    definedBy = "Person"
                )
            )
        )
    }

    fun `test going to declaration of variable used in each block`() {
        runGoToKotlinDeclarationTest(
            kotlinFileContent =
                // language=Kt
                """
                |data class Person(val name: String, val age: String)
                |data class ViewModel(val people: List<Person>)
                """.trimMargin(),
            handlebarsFileContent =
                // language=Handlebars
                """
                |{{#each people}}<h1>{{this.name}} is {{this.<caret>age}}</h1>{{/each}}
                """.trimMargin(),
            expectedReferences = listOf(
                ExpectedReference(
                    name = "age",
                    definedBy = "Person"
                )
            )
        )
    }

    fun `test going to declaration of variable used in named each block`() {
        runGoToKotlinDeclarationTest(
            kotlinFileContent =
                // language=Kt
                """
                |data class Person(val name: String, val age: String)
                |data class ViewModel(val people: List<Person>)
                """.trimMargin(),
            handlebarsFileContent =
                // language=Handlebars
                """
                |{{#each people as |person|}}<h1>{{person.<caret>age}}</h1>{{/each}}
                """.trimMargin(),
            expectedReferences = listOf(
                ExpectedReference(
                    name = "age",
                    definedBy = "Person"
                )
            )
        )
    }

    fun `test going to declaration of variable used in each block nested within an if block`() {
        runGoToKotlinDeclarationTest(
            kotlinFileContent =
                // language=Kt
                """
                |data class Person(val age: String)
                |data class ViewModel(val people: List<Person>)
                """.trimMargin(),
            handlebarsFileContent =
                """
                |{{#if people}}
                |    {{#each people}}
                |        <h1>{{this.<caret>age}}</h1>
                |    {{/each}}
                |{{/if}}
                """.trimMargin(),
            expectedReferences = listOf(
                ExpectedReference(
                    name = "age",
                    definedBy = "Person"
                )
            )
        )
    }

    fun `test going to declaration of variable that includes nesting`() {
        runGoToKotlinDeclarationTest(
            files = mapOf(
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
                "View.hbs" to
                        "<h1>{{<caret>person.name}}, world</h1>",
            ),
            expectedReferences = listOf(
                ExpectedReference(
                    name = "person",
                    definedBy = "ViewModel"
                )
            )
        )
    }

    fun `test going to declaration of variable that includes nesting - nested`() {
        runGoToKotlinDeclarationTest(
            files = mapOf(
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
                "View.hbs" to
                        "<h1>{{person.<caret>name}}, world</h1>",
            ),
            expectedReferences = listOf(
                ExpectedReference(
                    name = "name",
                    definedBy = "Person"
                )
            )
        )
    }

    fun `test going to declaration of variable that is a kotlin field, from within a partial`() {
        runGoToKotlinDeclarationTest(
            files = mapOf(
                "Person.kt" to
                        // language=Kt
                        """
                        |data class Person(val name: String)
                        """.trimMargin(),
                "Person.hbs" to
                        "<h1>{{<caret>name}}, world</h1>",
            ),
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

    private fun runGoToKotlinDeclarationTest(
        kotlinFileContent: String,
        handlebarsFileContent: String,
        expectedReferences: List<ExpectedReference>,
    ) = runGoToKotlinDeclarationTest(
        files = mapOf(
            "ViewModel.kt" to kotlinFileContent,
            "View.hbs" to handlebarsFileContent,
        ),
        expectedReferences = expectedReferences,
    )

    private fun runGoToKotlinDeclarationTest(
        files: Map<String, String>,
        expectedReferences: List<ExpectedReference>,
    ) {
        files.forEach { (fileName, content) -> myFixture.configureByText(fileName, content) }

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