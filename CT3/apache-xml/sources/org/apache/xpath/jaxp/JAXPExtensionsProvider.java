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

    public JAXPExtensionsProvider(XPathFunctionResolver resolver) {
        this.extensionInvocationDisabled = false;
        this.resolver = resolver;
        this.extensionInvocationDisabled = false;
    }

    public JAXPExtensionsProvider(XPathFunctionResolver resolver, boolean featureSecureProcessing) {
        this.extensionInvocationDisabled = false;
        this.resolver = resolver;
        this.extensionInvocationDisabled = featureSecureProcessing;
    }

    @Override
    public boolean functionAvailable(String ns, String funcName) throws TransformerException {
        try {
            if (funcName == null) {
                String fmsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"Function Name"});
                throw new NullPointerException(fmsg);
            }
            QName myQName = new QName(ns, funcName);
            XPathFunction xpathFunction = this.resolver.resolveFunction(myQName, 0);
            return xpathFunction != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean elementAvailable(String ns, String elemName) throws TransformerException {
        return false;
    }

    @Override
    public Object extFunction(String ns, String funcName, Vector argVec, Object methodKey) throws TransformerException {
        try {
            if (funcName == null) {
                String fmsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"Function Name"});
                throw new NullPointerException(fmsg);
            }
            QName myQName = new QName(ns, funcName);
            if (this.extensionInvocationDisabled) {
                String fmsg2 = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED, new Object[]{myQName.toString()});
                throw new XPathFunctionException(fmsg2);
            }
            int arity = argVec.size();
            XPathFunction xpathFunction = this.resolver.resolveFunction(myQName, arity);
            ArrayList argList = new ArrayList(arity);
            for (int i = 0; i < arity; i++) {
                ?? ElementAt = argVec.elementAt(i);
                if (ElementAt instanceof XNodeSet) {
                    argList.add(i, ElementAt.nodelist());
                } else if (ElementAt instanceof XObject) {
                    Object passedArgument = ElementAt.object();
                    argList.add(i, passedArgument);
                } else {
                    argList.add(i, ElementAt);
                }
            }
            return xpathFunction.evaluate(argList);
        } catch (XPathFunctionException xfe) {
            throw new WrappedRuntimeException(xfe);
        } catch (Exception e) {
            throw new TransformerException(e);
        }
    }

    @Override
    public Object extFunction(FuncExtFunction extFunction, Vector argVec) throws TransformerException {
        try {
            String namespace = extFunction.getNamespace();
            String functionName = extFunction.getFunctionName();
            int arity = extFunction.getArgCount();
            QName myQName = new QName(namespace, functionName);
            if (this.extensionInvocationDisabled) {
                String fmsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED, new Object[]{myQName.toString()});
                throw new XPathFunctionException(fmsg);
            }
            XPathFunction xpathFunction = this.resolver.resolveFunction(myQName, arity);
            ArrayList argList = new ArrayList(arity);
            for (int i = 0; i < arity; i++) {
                ?? ElementAt = argVec.elementAt(i);
                if (ElementAt instanceof XNodeSet) {
                    argList.add(i, ElementAt.nodelist());
                } else if (ElementAt instanceof XObject) {
                    Object passedArgument = ElementAt.object();
                    argList.add(i, passedArgument);
                } else {
                    argList.add(i, ElementAt);
                }
            }
            return xpathFunction.evaluate(argList);
        } catch (XPathFunctionException xfe) {
            throw new WrappedRuntimeException(xfe);
        } catch (Exception e) {
            throw new TransformerException(e);
        }
    }
}
