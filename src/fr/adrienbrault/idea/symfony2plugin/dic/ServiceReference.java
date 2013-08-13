package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.*;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class ServiceReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private String serviceId;

    public ServiceReference(@NotNull PsiElement element, String ServiceId) {
        super(element);
        serviceId = ServiceId;
    }

    public ServiceReference(@NotNull StringLiteralExpression element) {
        super(element);

        serviceId = element.getText().substring(
            element.getValueRange().getStartOffset(),
            element.getValueRange().getEndOffset()
        ); // Remove quotes
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        // Return the PsiElement for the class corresponding to the serviceId
        String serviceClass = ServiceXmlParserFactory.getInstance(getElement().getProject(), XmlServiceParser.class).getServiceMap().getMap().get(serviceId.toLowerCase());

        if (null == serviceClass) {
            return new ResolveResult[]{};
        }

        List<ResolveResult> resolveResults = PhpElementsUtil.getClassInterfaceResolveResult(this.getElement().getProject(), serviceClass);
        return resolveResults.toArray(new ResolveResult[resolveResults.size()]);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] resolveResults = multiResolve(false);

        return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        ServiceMap serviceMap = ServiceXmlParserFactory.getInstance(getElement().getProject(), XmlServiceParser.class).getServiceMap();
        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());

        List<LookupElement> results = new ArrayList<LookupElement>();
        Iterator it = serviceMap.getPublicMap().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            String serviceId = (String)pairs.getKey();
            String serviceClass = (String)pairs.getValue();
            Collection<PhpClass> phpClasses = phpIndex.getClassesByFQN(serviceClass);
            if (phpClasses.size() > 0) {
                results.add(new ServiceLookupElement(serviceId, phpClasses.iterator().next()));
            } else {
                Collection<PhpClass> phpInterfaces = phpIndex.getInterfacesByFQN(serviceClass);
                if (phpInterfaces.size() > 0) {
                    results.add(new ServiceLookupElement(serviceId, phpInterfaces.iterator().next()));
                }
            }
        }

        return results.toArray();
    }
}
