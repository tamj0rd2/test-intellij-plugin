package com.github.tamj0rd2.testintellijplugin.services

import com.dmarcotte.handlebars.psi.HbPsiFile
import com.dmarcotte.handlebars.psi.HbSimpleMustache
import com.github.tamj0rd2.testintellijplugin.MyBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import java.lang.IllegalStateException

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {
    private val searchScope = GlobalSearchScope.projectScope(project)
    private val psiShortNamesCache = PsiShortNamesCache.getInstance(project)

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    fun getRandomNumber() = (1..100).random()

    fun checkOneToOneMappingAgainstModel(hbsFile: HbPsiFile) {
        val fieldsRequiredByTemplate = hbsFile.findAllReferencedModelVariables()
        val viewModel = findCorrespondingKotlinModel(hbsFile.name)
        val fieldsDefinedByModel = viewModel.allFieldNames
        val missingFields = fieldsRequiredByTemplate - fieldsDefinedByModel

        if (missingFields.isNotEmpty()) {
            throw ViewModelMismatchException(
                templateName = hbsFile.name,
                viewModelName = viewModel.name!!,
                fieldsMissingFromViewModel = missingFields
            )
        }
    }

    data class ViewModelMismatchException(
        val templateName: String,
        val viewModelName: String,
        val fieldsMissingFromViewModel: Set<String>,
    ) : IllegalStateException(
        """
        |There was a mismatch between $templateName and ${viewModelName}.
        |These fields are required by the template but missing in the model:
        |[${fieldsMissingFromViewModel.joinToString(", ")}]
        """.trimMargin()
    )

    private fun HbPsiFile.findAllReferencedModelVariables(): Set<String> =
        PsiTreeUtil.collectElementsOfType(this, HbSimpleMustache::class.java).map { it.name }.toSet()

    private fun findCorrespondingKotlinModel(hbsFileName: String): KtUltraLightClass {
        val fileNameWithoutExtension = hbsFileName.substringBefore(".hbs")
        val expectedModelName = "${fileNameWithoutExtension}Model"
        return psiShortNamesCache.getClassesByName(expectedModelName, searchScope).firstIsInstance<KtUltraLightClass>()
    }

    private val KtUltraLightClass.allFieldNames: Set<String>
        get() = allFields.filterIsInstance<KtLightField>().map { it.name }.toSet()


}

