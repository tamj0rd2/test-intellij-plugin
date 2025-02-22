package com.github.tamj0rd2.testintellijplugin

import com.dmarcotte.handlebars.psi.HbPsiFile
import com.github.tamj0rd2.testintellijplugin.services.MyProjectService
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun `test validating one to one mapping`() {
        myFixture.configureByFiles("PageView.hbs", "PageViewModel.kt")

        val projectService = project.service<MyProjectService>()
        projectService.checkOneToOneMappingAgainstModel(assertInstanceOf(myFixture.configureByFile("PageView.hbs"), HbPsiFile::class.java))
    }

    fun `test going to declaration of variable`() {
        myFixture.configureByFiles("PageView.hbs", "PageViewModel.kt")

        val targetElements = GotoDeclarationAction.findAllTargetElements(project, myFixture.editor, 37)
        UsefulTestCase.assertSize(1, targetElements)
    }

    override fun getTestDataPath() = "src/test/testData/rename"
}
