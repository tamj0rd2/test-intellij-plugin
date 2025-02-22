package com.github.tamj0rd2.testintellijplugin

import com.dmarcotte.handlebars.psi.HbPsiFile
import com.github.tamj0rd2.testintellijplugin.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MyPluginTest : BasePlatformTestCase() {

    fun `test validating one to one mapping`() {
        myFixture.configureByText(
            "ViewModel.kt",
            // language=Kt
            """
            |interface InheritedFields {
            |    val inheritedComputedProperty get() = "some inherited computed property"
            |    val inheritedField: Boolean
            |    val anotherInheritedField: Int
            |}
            |
            |data class ViewModel(
            |    val aField: String,
            |    override val inheritedField: Boolean,
            |) : InheritedFields {
            |    val aComputedProperty get() = "some computed property"
            |    val aHardCodedProperty = 123
            |    val anotherInheritedField = 456
            |}
            """.trimMargin()
        )

        val hbsFile = myFixture.configureByText(
            "View.hbs",
            // language=Handlebars
            """
            |<p><{{aField}}/p>
            |<p><{{aComputedProperty}}/p>
            |<p>{{inheritedComputedProperty}}</p>
            |<p>This one doesn't exist in the view model: {{nonExistentField}}</p>
            """.trimMargin()
        ) as HbPsiFile

        val projectService = project.service<MyProjectService>()
        val result = projectService.validateOneToOneMappingAgainstViewModel(hbsFile)
        assertEquals(setOf("nonExistentField"), result.fieldsMissingFromViewModel)
    }
}
