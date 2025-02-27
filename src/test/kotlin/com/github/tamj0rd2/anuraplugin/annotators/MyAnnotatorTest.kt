package com.github.tamj0rd2.anuraplugin.annotators

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase

class MyAnnotatorTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "path/to/your/testdata" // Set the path to your test data files
    }

    fun testMyAnnotator() {
        setupFiles(
            files = mapOf(
                "ViewModel.kt" to
                    // language=Kt
                    """
                    |data class ViewModel(val greeting: String, val name: String)
                        """.trimMargin(),
                "View.hbs" to
                    // language=Handlebars
                    "<h1>{{greeting}}, {{incorrectName}}</h1>",
            ),
        )

        val highlightedTexts = myFixture.doHighlighting().map { it.text }.toSet()
        TestCase.assertEquals(setOf("incorrectName"), highlightedTexts)
    }

    private fun setupFiles(files: Map<String, String>) {
        files.forEach { (fileName, content) -> myFixture.configureByText(fileName, content) }
    }
}