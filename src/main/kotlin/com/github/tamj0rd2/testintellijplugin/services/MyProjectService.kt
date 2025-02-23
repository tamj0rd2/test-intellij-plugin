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
import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.asJava.classes.KtLightClassBase
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

interface IMyProjectService {
    fun findKotlinReferences(hbsFileName: String, hbsIdentifierParts: List<String>): Collection<KtDeclaration>
}

@Service(Service.Level.PROJECT)
class MyProjectService(private val project: Project) : IMyProjectService {
    private val psiShortNamesCache get() = PsiShortNamesCache.getInstance(project)

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }

    fun validateOneToOneMappingAgainstViewModel(hbsFile: HbPsiFile): MappingValidationResult {
        val fieldsRequiredByTemplate = hbsFile.findAllReferencedModelVariables()
        val viewModelName = hbsFileNameToKotlinModelName(hbsFile.name)
        val viewModel = findKotlinClassesByName(viewModelName).single()

        return MappingValidationResult(
            fieldsInModel = viewModel.allFieldsAndProperties.mapNotNull { it.name }.toSet(),
            fieldsRequiredByTemplate = fieldsRequiredByTemplate
        )
    }

    override fun findKotlinReferences(hbsFileName: String, hbsIdentifierParts: List<String>): Collection<KtDeclaration> {
        return recursivelyFindMatchingKotlinReferences(
            typesToSearchIn = setOf(hbsFileNameToKotlinModelName(hbsFileName)),
            hbsIdentifierParts = hbsIdentifierParts
        )
    }


    private tailrec fun recursivelyFindMatchingKotlinReferences(
        typesToSearchIn: Set<String>,
        hbsIdentifierParts: List<String>,
    ): Collection<KtDeclaration> {
        require(hbsIdentifierParts.isNotEmpty()) { "the list of hbs identifier parts shouldn't be empty." }

        val models = typesToSearchIn.flatMap(::findKotlinClassesByName).distinctBy { it.node }

        val matchingFields = models
            .flatMap { it.allFieldsAndProperties }
            .filter { it.name == hbsIdentifierParts.first() }

        if (hbsIdentifierParts.size == 1) return matchingFields

        return recursivelyFindMatchingKotlinReferences(
            typesToSearchIn = matchingFields.map { it.nameOfReferencedType }.toSet(),
            hbsIdentifierParts = hbsIdentifierParts.drop(1)
        )
    }

    data class MappingValidationResult(
        val fieldsInModel: Set<String>,
        val fieldsRequiredByTemplate: Set<String>,
    ) {
        val fieldsMissingFromViewModel = fieldsRequiredByTemplate - fieldsInModel
    }

    private fun findKotlinClassesByName(modelName: String): List<KtLightClassBase> {
        return psiShortNamesCache.getClassesByName(
            modelName,
            // NOTE: performance could be improved here by not using the scope of the entire project.
            GlobalSearchScope.projectScope(project)
        ).filterIsInstance<KtLightClassBase>().distinctBy { it.node }
    }

    private companion object {
        fun HbPsiFile.findAllReferencedModelVariables(): Set<String> =
            PsiTreeUtil.collectElementsOfType(this, HbSimpleMustache::class.java).map { it.name }.toSet()

        val KtDeclaration.nameOfReferencedType: String
            get() = typeReference!!.descendantsOfType<KtNameReferenceExpression>().first().getReferencedName()

        val KtLightClassBase.allFieldsAndProperties: List<KtDeclaration>
            get() = allProperties + allFields.filterIsInstance<KtLightField>().mapNotNull { it.kotlinOrigin }

        val KtLightClassBase.allProperties: List<KtProperty>
            get() = allMethods
                .filterIsInstance<KtLightMethod>()
                .map { it.kotlinOrigin }
                .filterIsInstance<KtProperty>()

        val KtDeclaration.typeReference get() = when(this) {
            is KtParameter -> this.typeReference
            is KtProperty -> this.typeReference
            else -> error("unsupported type ${this::class.java}")
        }

        private fun hbsFileNameToKotlinModelName(hbsFileName: String) = hbsFileName.substringBefore(".hbs") + "Model"
    }
}
