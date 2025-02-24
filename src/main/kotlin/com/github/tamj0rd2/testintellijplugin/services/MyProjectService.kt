package com.github.tamj0rd2.testintellijplugin.services

import com.dmarcotte.handlebars.psi.HbPsiFile
import com.dmarcotte.handlebars.psi.HbSimpleMustache
import com.github.tamj0rd2.testintellijplugin.MyBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
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
    fun findKotlinReferences(hbsFile: VirtualFile, hbsIdentifierParts: List<String>): Collection<KtDeclaration>
}

@Service(Service.Level.PROJECT)
class MyProjectService(private val project: Project) : IMyProjectService {
    private val psiShortNamesCache get() = PsiShortNamesCache.getInstance(project)

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }

    fun validateOneToOneMappingAgainstViewModel(hbsFile: HbPsiFile): MappingValidationResult {
        val fieldsRequiredByTemplate = hbsFile.findAllReferencedModelVariables()
        val viewModelName = hbsFileToKotlinModelName(hbsFile.virtualFile)
        val viewModel = findKotlinClassesByName(
            modelName = viewModelName,
            scope = GlobalSearchScope.projectScope(project)
        ).single()

        return MappingValidationResult(
            fieldsInModel = viewModel.allFieldsAndProperties.mapNotNull { it.name }.toSet(),
            fieldsRequiredByTemplate = fieldsRequiredByTemplate
        )
    }

    override fun findKotlinReferences(
        hbsFile: VirtualFile,
        hbsIdentifierParts: List<String>,
    ): Collection<KtDeclaration> {
        val scope = hbsFile.toKotlinProductionScopeOrDefault()

        return recursivelyFindMatchingKotlinReferences(
            typesToSearchIn = setOf(hbsFileToKotlinModelName(hbsFile)),
            hbsIdentifierParts = hbsIdentifierParts,
            scope = scope
        )
    }

    private tailrec fun recursivelyFindMatchingKotlinReferences(
        typesToSearchIn: Set<String>,
        hbsIdentifierParts: List<String>,
        scope: GlobalSearchScope,
    ): Collection<KtDeclaration> {
        require(hbsIdentifierParts.isNotEmpty()) { "the list of hbs identifier parts shouldn't be empty." }

        val models = typesToSearchIn
            .flatMap { findKotlinClassesByName(it, scope) }
            .distinctBy { it.node }

        val matchingFields = models
            .flatMap { it.allFieldsAndProperties }
            .filter { it.name == hbsIdentifierParts.first() }

        if (hbsIdentifierParts.size == 1) return matchingFields

        return recursivelyFindMatchingKotlinReferences(
            typesToSearchIn = matchingFields.map { it.nameOfReferencedType }.toSet(),
            hbsIdentifierParts = hbsIdentifierParts.drop(1),
            scope = scope,
        )
    }

    data class MappingValidationResult(
        val fieldsInModel: Set<String>,
        val fieldsRequiredByTemplate: Set<String>,
    ) {
        val fieldsMissingFromViewModel = fieldsRequiredByTemplate - fieldsInModel
    }

    private fun findKotlinClassesByName(modelName: String, scope: GlobalSearchScope): List<KtLightClassBase> {
        return psiShortNamesCache.getClassesByName(modelName, scope)
            .filterIsInstance<KtLightClassBase>()
            .distinctBy { it.node }
    }

    private fun VirtualFile.toKotlinProductionScopeOrDefault(): GlobalSearchScope {
        if (!path.contains("src/main/resources")) return GlobalSearchScope.projectScope(project)

        var folderToSearch = this
        while(!folderToSearch.path.endsWith("src/main")) {
            folderToSearch = folderToSearch.parent
        }
        folderToSearch = folderToSearch.resolveFromRootOrRelative("kotlin")!!

        return GlobalSearchScopesCore.directoryScope(project, folderToSearch, true)
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

        val KtDeclaration.typeReference
            get() = when (this) {
                is KtParameter -> this.typeReference
                is KtProperty -> this.typeReference
                else -> error("unsupported type ${this::class.java}")
            }

        private fun hbsFileToKotlinModelName(hbsFile: VirtualFile): String {
            if (hbsFile.nameWithoutExtension.endsWith("View")) {
                return hbsFile.nameWithoutExtension + "Model"
            }

            return hbsFile.nameWithoutExtension
        }
    }
}
