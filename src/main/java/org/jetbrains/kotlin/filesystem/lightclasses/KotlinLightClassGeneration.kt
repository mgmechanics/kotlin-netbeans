/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.filesystem.lightclasses

import java.io.File
import java.util.regex.Pattern
import org.jetbrains.kotlin.fileClasses.*
import org.jetbrains.kotlin.filesystem.KotlinLightClassManager
import org.jetbrains.kotlin.log.KotlinLogger
import org.jetbrains.kotlin.model.KotlinEnvironment
import org.jetbrains.kotlin.utils.ProjectUtils
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.BindingContext
import org.netbeans.api.project.Project
import org.openide.filesystems.FileObject

object KotlinLightClassGeneration {

    fun buildLightClasses(analysisResult: AnalysisResult, project: Project,
                          ktFiles: List<KtFile>, requestedClassName: String): GenerationState? {
        val generateDeclaredClassFilter = object : GenerationState.GenerateClassFilter() {
            override fun shouldAnnotateClass(classOrObject: KtClassOrObject) = true

            override fun shouldGenerateClass(classOrObject: KtClassOrObject) = true

            override fun shouldGeneratePackagePart(ktFile: KtFile) = true

            override fun shouldGenerateScript(script: KtScript) = false
            
        }
        
        val state = GenerationState(
                KotlinEnvironment.getEnvironment(project).project,
                LightClassBuilderFactory(),
                analysisResult.moduleDescriptor,
                analysisResult.bindingContext,
                ktFiles,
                true,
                true,
                generateDeclaredClassFilter)
        
        KotlinCodegenFacade.compileCorrectFiles(state, {ex, url -> Unit})
        
        return state
    }

    private fun checkByInternalName(internalName: String?, requestedClassFileName: String): Boolean {
        if (internalName == null) return false
        
        val classFileName = getLastSegment(internalName)
        val requestedInternalName = requestedClassFileName.dropLast(".class".length)
        
        if (requestedInternalName.startsWith(classFileName)) {
            if (requestedInternalName.length == classFileName.length) return true
            
            if (requestedInternalName[classFileName.length] == '$') return true
        }
        
        return false
    }

    private fun getLastSegment(path: String) = path.substringAfterLast("/")

    fun getByteCode(file: FileObject, project: Project?, 
                    analysisResult: AnalysisResult): List<ByteArray> {
        if (project == null) return emptyList()
        
        val code = arrayListOf<ByteArray>()
        
        val manager = KotlinLightClassManager.getInstance(project)
        manager.computeLightClassesSources()
        
        manager.getLightClassesPaths(file).forEach {
            val lightClass = File(ProjectUtils.getKotlinProjectLightClassesPath(project) 
                    + ProjectUtils.FILE_SEPARATOR + it)
            val ktFiles = manager.getSourceFiles(lightClass)
            val className = it.substringAfterLast(Pattern.quote(ProjectUtils.FILE_SEPARATOR))
            if (ktFiles.isNotEmpty()) {
                val state = buildLightClasses(analysisResult, project, ktFiles, className) ?: return emptyList()
                
                state.factory.asList().forEach { outFile -> code.add(outFile.asByteArray()) }
            }
        }
        
        return code
    }
}