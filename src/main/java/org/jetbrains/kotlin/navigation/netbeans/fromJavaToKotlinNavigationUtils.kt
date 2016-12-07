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
package org.jetbrains.kotlin.navigation.netbeans

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.io.IOException
import javax.lang.model.element.ElementKind
import javax.swing.text.Document
import kotlin.Pair
import org.jetbrains.kotlin.utils.ProjectUtils
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.filesystem.lightclasses.LightClassBuilderFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.lang.java.ElementHandleFieldContainingClassSearcher
import org.jetbrains.kotlin.resolve.lang.java.ElementHandleNameSearcher
import org.jetbrains.kotlin.resolve.lang.java.ElementHandleSimpleNameSearcher
import org.jetbrains.kotlin.resolve.lang.java.ElementSearcher
import org.netbeans.api.java.source.JavaSource
import org.netbeans.api.java.source.SourceUtils
import org.netbeans.api.java.source.ElementHandle
import org.netbeans.api.project.Project
import org.openide.util.Exceptions

fun getElement(doc: Document?, offset: Int): ElementHandle<*>? {
    val javaSource = JavaSource.forDocument(doc) ?: return null
    val searcher = ElementSearcher(offset)
    try {
        javaSource.runUserActionTask(searcher, true)
    } catch (ex: IOException) {
        Exceptions.printStackTrace(ex)
    }

    return searcher.element
}

fun findKotlinFileToNavigate(element: ElementHandle<*>?, project: Project?, doc: Document): Pair<KtFile, Int>? {
    if (element == null || project == null) return null

    val ktFiles = ProjectUtils.getSourceFiles(project)

    ktFiles.forEach {
        val ktElement = findKotlinDeclaration(element, it, doc)
        if (ktElement != null) {
            val offset = ktElement.textOffset
            return Pair(it, offset)
        }
    }

    return null
}

private fun findKotlinDeclaration(element: ElementHandle<*>, ktFile: KtFile, doc: Document): KtElement? {
    val result = arrayListOf<KtElement>()
    val visitor = makeVisitor(element, result, doc)
    if (visitor != null) {
        ktFile.acceptChildren(visitor)
    }

    return result.firstOrNull()
}

private fun makeVisitor(element: ElementHandle<*>, result: MutableList<KtElement>, doc: Document): KtVisitorVoid? {
    when (element.kind) {
        ElementKind.CLASS,
        ElementKind.INTERFACE,
        ElementKind.ENUM -> return object : KtAllVisitor() {

            override fun visitClassOrObject(ktClassOrObject: KtClassOrObject) {
                val fqName = ktClassOrObject.fqName?.asString() ?: ""
                if (fqName.equals(element.getQualifiedName())) {
                    result.add(ktClassOrObject)
                    return
                }
                ktClassOrObject.acceptChildren(this)
            }
        }
        ElementKind.FIELD -> return object : KtAllVisitor() {

            override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
                visitExplicitDeclaration(declaration)
                declaration.acceptChildren(this)
            }

            override fun visitEnumEntry(enumEntry: KtEnumEntry) {
                visitExplicitDeclaration(enumEntry)
            }

            override fun visitProperty(property: KtProperty) {
                visitExplicitDeclaration(property)
            }

            private fun visitExplicitDeclaration(declaration: KtDeclaration?) {
                if (declaration == null) return
                val javaSource = JavaSource.forDocument(doc) ?: return
                val searcher = ElementHandleNameSearcher(element)
                try {
                    javaSource.runUserActionTask(searcher, true)
                } catch (ex: IOException) {
                    Exceptions.printStackTrace(ex)
                }
                if (equalsNames(declaration, element, doc) && declaration.name.equals(searcher.name.asString())) {
                    result.add(declaration)
                }
            }
        }
        ElementKind.METHOD,
        ElementKind.CONSTRUCTOR -> return object : KtAllVisitor() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                visitExplicitDeclaration(function)
            }

            override fun visitProperty(property: KtProperty) {
                visitExplicitDeclaration(property)
                property.acceptChildren(this)
            }

            override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
                visitExplicitDeclaration(accessor)
            }

            override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
                visitExplicitDeclaration(constructor)
            }

            override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
                visitExplicitDeclaration(constructor)
            }

            override fun visitClass(ktClass: KtClass) {
                val javaSource = JavaSource.forDocument(doc) ?: return
                val searcher = ElementHandleFieldContainingClassSearcher(element)
                try {
                    javaSource.runUserActionTask(searcher, true)
                } catch (ex: IOException) {
                    Exceptions.printStackTrace(ex)
                }
                val containingClass = searcher.containingClass
                val fqName = containingClass?.qualifiedName ?: ""
                if (equalsNames(ktClass, element, doc) && (fqName.equals(ktClass.fqName?.asString()))) {
                    result.add(ktClass)
                    return
                }
                ktClass.acceptChildren(this)
            }

            private fun visitExplicitDeclaration(declaration: KtDeclaration?) {
                if (declaration == null) return

                if (equalsNames(declaration, element, doc) && equalsDeclaringTypes(declaration, element, doc)) {
                    result.add(declaration)
                }
            }
        }
        else -> return null
    }
}

fun equalsNames(ktElement: KtElement?, element: ElementHandle<*>?, doc: Document): Boolean {
    if (ktElement == null || element == null) return false
    val first = ktElement.getName() ?: return false
    val javaSource = JavaSource.forDocument(doc) ?: return false
    val searcher = ElementHandleSimpleNameSearcher(element)
    try {
        javaSource.runUserActionTask(searcher, true)
    } catch (ex: IOException) {
        Exceptions.printStackTrace(ex)
    }
    val second = searcher.simpleName ?: return false

    if (first != second) return false

    val ktSignatures: Set<Pair<String, String>> =
            ktElement.getUserData(LightClassBuilderFactory.JVM_SIGNATURE) ?: return false

    val signatures = SourceUtils.getJVMSignature(element).toList()

    ktSignatures.firstOrNull { signatures.contains(it.first) } ?: return false
    return true
}

fun equalsDeclaringTypes(ktElement: KtElement?, element: ElementHandle<*>?, doc: Document): Boolean {
    if (ktElement == null || element == null) return false

    val typeNameInfo = getDeclaringTypeFqName(ktElement) ?: return false
    val javaSource = JavaSource.forDocument(doc) ?: return false
    val searcher = ElementHandleFieldContainingClassSearcher(element)
    try {
        javaSource.runUserActionTask(searcher, true)
    } catch (ex: IOException) {
        Exceptions.printStackTrace(ex)
    }
    val containingClass = searcher.containingClass ?: return false
    val fqName = containingClass.qualifiedName ?: return false
    return fqName == typeNameInfo.asString() || typeNameInfo.asString() == "${fqName}Kt"
}

private fun getDeclaringTypeFqName(ktElement: KtElement?): FqName? {
    val parent = PsiTreeUtil.getParentOfType(ktElement,
            KtClassOrObject::class.java, KtFile::class.java) ?: return null
    return getTypeFqName(parent)
}

private fun getTypeFqName(element: PsiElement?) = when (element) {
    is KtClassOrObject -> element.fqName
    is KtFile -> NoResolveFileClassesProvider.getFileClassInfo(element).fileClassFqName
    else -> null
}

private open class KtAllVisitor : KtVisitorVoid() {
    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }
}