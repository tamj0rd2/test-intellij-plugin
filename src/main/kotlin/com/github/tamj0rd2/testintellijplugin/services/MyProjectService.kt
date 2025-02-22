package com.github.tamj0rd2.testintellijplugin.services

import com.dmarcotte.handlebars.psi.HbPsiFile
import com.dmarcotte.handlebars.psi.HbSimpleMustache
import com.github.tamj0rd2.testintellijplugin.MyBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiField
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

interface IMyProjectService {
    fun findReferenceInKotlin(hbsFile: HbPsiFile, hbsVariableName: String): Collection<PsiField>
}

@Service(Service.Level.PROJECT)
class MyProjectService(private val project: Project) : IMyProjectService {
    private val psiShortNamesCache get() = PsiShortNamesCache.getInstance(project)

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }

    fun validateOneToOneMappingAgainstViewModel(hbsFile: HbPsiFile): MappingValidationResult {
        val fieldsRequiredByTemplate = hbsFile.findAllReferencedModelVariables()
        val viewModel = findCorrespondingKotlinModel(hbsFile.name)

        return MappingValidationResult(
            fieldsInModel = viewModel.allFieldNames,
            fieldsRequiredByTemplate = fieldsRequiredByTemplate
        )
    }

    override fun findReferenceInKotlin(hbsFile: HbPsiFile, hbsVariableName: String): Collection<PsiField> {
        val model = findCorrespondingKotlinModel(hbsFile.name)
        return model.fields.filter { it.name == hbsVariableName }
    }

    data class MappingValidationResult(
        val fieldsInModel: Set<String>,
        val fieldsRequiredByTemplate: Set<String>,
    ) {
        val fieldsMissingFromViewModel = fieldsRequiredByTemplate - fieldsInModel
    }

    private fun HbPsiFile.findAllReferencedModelVariables(): Set<String> =
        PsiTreeUtil.collectElementsOfType(this, HbSimpleMustache::class.java).map { it.name }.toSet()

    private fun findCorrespondingKotlinModel(hbsFileName: String): KtUltraLightClass {
        val fileNameWithoutExtension = hbsFileName.substringBefore(".hbs")
        val expectedModelName = "${fileNameWithoutExtension}Model"
        return psiShortNamesCache.getClassesByName(
            expectedModelName,
            // NOTE: performance could be improved here by not using the scope of the entire project.
            GlobalSearchScope.projectScope(project)
        ).firstIsInstance<KtUltraLightClass>()
    }

    private val KtUltraLightClass.allFieldNames: Set<String>
        get() = allFields.filterIsInstance<KtLightField>().map { it.name }.toSet()
}

