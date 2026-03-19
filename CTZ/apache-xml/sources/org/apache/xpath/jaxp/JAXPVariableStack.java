package org.apache.xpath.jaxp;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathVariableResolver;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.utils.QName;
import org.apache.xpath.VariableStack;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.res.XPATHErrorResources;

public class JAXPVariableStack extends VariableStack {
    private final XPathVariableResolver resolver;

    public JAXPVariableStack(XPathVariableResolver xPathVariableResolver) {
        super(2);
        this.resolver = xPathVariableResolver;
    }

    @Override
    public XObject getVariableOrParam(XPathContext xPathContext, QName qName) throws TransformerException, IllegalArgumentException {
        if (qName == null) {
            throw new IllegalArgumentException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"Variable qname"}));
        }
        javax.xml.namespace.QName qName2 = new javax.xml.namespace.QName(qName.getNamespace(), qName.getLocalPart());
        Object objResolveVariable = this.resolver.resolveVariable(qName2);
        if (objResolveVariable == null) {
            throw new TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_RESOLVE_VARIABLE_RETURNS_NULL, new Object[]{qName2.toString()}));
        }
        return XObject.create(objResolveVariable, xPathContext);
    }
}
