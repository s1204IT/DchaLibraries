package org.apache.xpath.jaxp;

import java.util.ArrayList;
import java.util.Vector;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.ExtensionsProvider;
import org.apache.xpath.functions.FuncExtFunction;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.res.XPATHErrorResources;

public class JAXPExtensionsProvider implements ExtensionsProvider {
    private boolean extensionInvocationDisabled;
    private final XPathFunctionResolver resolver;

    public JAXPExtensionsProvider(XPathFunctionResolver xPathFunctionResolver) {
        this.extensionInvocationDisabled = false;
        this.resolver = xPathFunctionResolver;
        this.extensionInvocationDisabled = false;
    }

    public JAXPExtensionsProvider(XPathFunctionResolver xPathFunctionResolver, boolean z) {
        this.extensionInvocationDisabled = false;
        this.resolver = xPathFunctionResolver;
        this.extensionInvocationDisabled = z;
    }

    @Override
    public boolean functionAvailable(String str, String str2) throws TransformerException {
        try {
            if (str2 != null) {
                return this.resolver.resolveFunction(new QName(str, str2), 0) != null;
            }
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"Function Name"}));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean elementAvailable(String str, String str2) throws TransformerException {
        return false;
    }

    @Override
    public Object extFunction(String str, String str2, Vector vector, Object obj) throws TransformerException {
        try {
            if (str2 == null) {
                throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"Function Name"}));
            }
            QName qName = new QName(str, str2);
            if (this.extensionInvocationDisabled) {
                throw new XPathFunctionException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED, new Object[]{qName.toString()}));
            }
            int size = vector.size();
            XPathFunction xPathFunctionResolveFunction = this.resolver.resolveFunction(qName, size);
            ArrayList arrayList = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                Object objElementAt = vector.elementAt(i);
                if (objElementAt instanceof XNodeSet) {
                    arrayList.add(i, ((XNodeSet) objElementAt).nodelist());
                } else if (objElementAt instanceof XObject) {
                    arrayList.add(i, ((XObject) objElementAt).object());
                } else {
                    arrayList.add(i, objElementAt);
                }
            }
            return xPathFunctionResolveFunction.evaluate(arrayList);
        } catch (XPathFunctionException e) {
            throw new WrappedRuntimeException(e);
        } catch (Exception e2) {
            throw new TransformerException(e2);
        }
    }

    @Override
    public Object extFunction(FuncExtFunction funcExtFunction, Vector vector) throws TransformerException {
        try {
            String namespace = funcExtFunction.getNamespace();
            String functionName = funcExtFunction.getFunctionName();
            int argCount = funcExtFunction.getArgCount();
            QName qName = new QName(namespace, functionName);
            if (this.extensionInvocationDisabled) {
                throw new XPathFunctionException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED, new Object[]{qName.toString()}));
            }
            XPathFunction xPathFunctionResolveFunction = this.resolver.resolveFunction(qName, argCount);
            ArrayList arrayList = new ArrayList(argCount);
            for (int i = 0; i < argCount; i++) {
                Object objElementAt = vector.elementAt(i);
                if (objElementAt instanceof XNodeSet) {
                    arrayList.add(i, ((XNodeSet) objElementAt).nodelist());
                } else if (objElementAt instanceof XObject) {
                    arrayList.add(i, ((XObject) objElementAt).object());
                } else {
                    arrayList.add(i, objElementAt);
                }
            }
            return xPathFunctionResolveFunction.evaluate(arrayList);
        } catch (XPathFunctionException e) {
            throw new WrappedRuntimeException(e);
        } catch (Exception e2) {
            throw new TransformerException(e2);
        }
    }
}
