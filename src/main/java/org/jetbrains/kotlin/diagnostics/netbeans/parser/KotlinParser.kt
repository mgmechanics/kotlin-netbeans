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
package org.jetbrains.kotlin.diagnostics.netbeans.parser

import javax.swing.event.ChangeListener
import org.jetbrains.kotlin.log.KotlinLogger
import org.jetbrains.kotlin.resolve.AnalysisResultWithProvider
import org.jetbrains.kotlin.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.utils.ProjectUtils
import org.jetbrains.kotlin.psi.KtFile
import org.netbeans.api.project.Project
import org.netbeans.modules.parsing.api.Snapshot
import org.netbeans.modules.parsing.api.Task
import org.netbeans.modules.parsing.spi.Parser
import org.netbeans.modules.parsing.spi.Parser.Result
import org.netbeans.modules.parsing.spi.SourceModificationEvent

class KotlinParser : Parser() {
    
    companion object {
        @JvmStatic var file: KtFile? = null
            private set
        
        @JvmStatic private val CACHE = hashMapOf<String, AnalysisResultWithProvider>()
        
        @JvmStatic fun getAnalysisResult() = if (file != null) CACHE[file!!.virtualFile.path] else null
        
        @JvmStatic fun getAnalysisResult(ktFile: KtFile) = CACHE[ktFile.virtualFile.path]
        
        // for tests only
        @JvmStatic fun setAnalysisResult(ktFile: KtFile, analysisResult: AnalysisResultWithProvider) {
            file = ktFile
            CACHE.put(ktFile.virtualFile.path, analysisResult)
        }
    }
    
    lateinit var snapshot: Snapshot
    private var project: Project? = null

    override fun parse(snapshot: Snapshot, task: Task, event: SourceModificationEvent) {
        this.snapshot = snapshot
        project = ProjectUtils.getKotlinProjectForFileObject(snapshot.source.fileObject)
        if (project == null) {
            file = null
            return
        }
        
        file = ProjectUtils.getKtFile(snapshot.text.toString(), snapshot.source.fileObject)
        if (event.affectedEndOffset <= 0) {
            CACHE.put(file!!.virtualFile.path, KotlinAnalysisProjectCache.INSTANCE.getAnalysisResult(project))
            return
        }
        
        CACHE.put(file!!.virtualFile.path, KotlinAnalyzer.analyzeFile(project!!, file!!))
    }
    
    override fun getResult(task: Task): Result? {
        if (project != null && file != null) return KotlinParserResult(snapshot, CACHE[file!!.virtualFile.path]!!, file!!, project!!)
        
        return null
    }
    
    override fun addChangeListener(changeListener: ChangeListener) {}
    override fun removeChangeListener(changeListener: ChangeListener) {}
    
    override fun cancel(reason: CancelReason, event: SourceModificationEvent?) {
        KotlinLogger.INSTANCE.logInfo("Parser cancel ${reason.name}")
    }
}