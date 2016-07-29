package org.black.kotlin.highlighter.occurrences;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.black.kotlin.diagnostics.netbeans.parser.KotlinParser.KotlinParserResult;
import org.black.kotlin.navigation.references.ReferenceUtils;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;
import org.netbeans.modules.csl.api.ColoringAttributes;
import org.netbeans.modules.csl.api.OccurrencesFinder;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;

/**
 *
 * @author Александр
 */
public class KotlinOccurrencesFinder extends OccurrencesFinder<KotlinParserResult> {

    private int caretPosition = 0;
    boolean cancel = false;
    Map<OffsetRange, ColoringAttributes> highlighting = new HashMap<OffsetRange, ColoringAttributes>();
    
    @Override
    public void setCaretPosition(int position) {
        caretPosition = position;
    }

    @Override
    public Map<OffsetRange, ColoringAttributes> getOccurrences() {
        return highlighting;
    }

    @Override
    public void run(KotlinParserResult result, SchedulerEvent event) {
        cancel = false;
        highlighting.clear();
        KtFile ktFile = result.getKtFile();
        
        PsiElement psiElement = ktFile.findElementAt(caretPosition);
        
        KtElement ktElement = PsiTreeUtil.getNonStrictParentOfType(psiElement, KtElement.class);
        
       List<KtElement> occurrences = searchTextOccurrences(ktFile, ktElement);
       for (KtElement element : occurrences) {
           OffsetRange range = new OffsetRange(element.getTextRange().getStartOffset(), 
                   element.getTextRange().getEndOffset());
           highlighting.put(range, ColoringAttributes.MARK_OCCURRENCES);
       }
    }

    private List<PsiElement> findOccurrences(KtFile ktFile, KtElement ktElement) {
        List<? extends SourceElement> sourceElements = ReferenceUtils.resolveToSourceDeclaration(ktElement);
        
        if (sourceElements.isEmpty()) {
            return null;
        }
        
        List<? extends SourceElement> searchingElements = getSearchingElements(sourceElements);
        
        
        return new ArrayList<PsiElement>();
    }
    
    public List<KtElement> searchTextOccurrences(KtFile ktFile, KtElement sourceElement) {
        List<KtElement> elements = new ArrayList<KtElement>();
        List<KtElement> elementsToReturn = new ArrayList<KtElement>();
        for (PsiElement psi : ktFile.getChildren()) {
            if (psi.textMatches(sourceElement)) {
                KtElement el = PsiTreeUtil.getNonStrictParentOfType(psi, KtElement.class);
                if (el != null) {
                    elements.add(el);
                }
            }
        }
        
        List<SearchFilter> beforeResolveFilters = SearchUtils.getBeforeResolveFilters();
        List<? extends SearchFilterAfterResolve> afterResolveFilters = SearchUtils.getAfterResolveFilters();
        
        for (KtElement element : elements) {
            boolean beforeResolveCheck = true;
            for (SearchFilter filter : beforeResolveFilters) {
                if (!filter.isApplicable(element)) {
                    beforeResolveCheck = false;
                    break;
                }
            }
            if (!beforeResolveCheck) {
                continue;
            }
            
            List<? extends SourceElement> sourceElements = ReferenceUtils.resolveToSourceDeclaration(element);
            if (sourceElements.isEmpty()) {
                continue;
            }
            
            List<SourceElement> additionalElements = 
                    ReferenceUtils.getContainingClassOrObjectForConstructor(sourceElements);
            
            for (SearchFilterAfterResolve filter : afterResolveFilters) {
                if (filter.isApplicable(sourceElements, sourceElement) 
                        || filter.isApplicable(additionalElements, sourceElement)) {
                    elementsToReturn.add(element);
                }
            }
            
        }
        
        return elementsToReturn;
    }
    
    public List<? extends SourceElement> getSearchingElements(List<? extends SourceElement> sourceElements) {
        List<SourceElement> classOrObjects = ReferenceUtils.getContainingClassOrObjectForConstructor(sourceElements);
        return classOrObjects.isEmpty() ? sourceElements : classOrObjects;
    } 
    
    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {
        cancel = true;
    }
}
