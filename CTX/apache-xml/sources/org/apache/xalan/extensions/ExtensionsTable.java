package org.apache.xalan.extensions;

import java.util.Hashtable;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xpath.XPathProcessorException;
import org.apache.xpath.functions.FuncExtFunction;

public class ExtensionsTable {
    public Hashtable m_extensionFunctionNamespaces = new Hashtable();
    private StylesheetRoot m_sroot;

    public ExtensionsTable(StylesheetRoot stylesheetRoot) throws TransformerException {
        this.m_sroot = stylesheetRoot;
        Vector extensions = this.m_sroot.getExtensions();
        for (int i = 0; i < extensions.size(); i++) {
            ExtensionNamespaceSupport extensionNamespaceSupport = (ExtensionNamespaceSupport) extensions.get(i);
            ExtensionHandler extensionHandlerLaunch = extensionNamespaceSupport.launch();
            if (extensionHandlerLaunch != null) {
                addExtensionNamespace(extensionNamespaceSupport.getNamespace(), extensionHandlerLaunch);
            }
        }
    }

    public ExtensionHandler get(String str) {
        return (ExtensionHandler) this.m_extensionFunctionNamespaces.get(str);
    }

    public void addExtensionNamespace(String str, ExtensionHandler extensionHandler) {
        this.m_extensionFunctionNamespaces.put(str, extensionHandler);
    }

    public boolean functionAvailable(String str, String str2) throws TransformerException {
        ExtensionHandler extensionHandler;
        if (str != null && (extensionHandler = (ExtensionHandler) this.m_extensionFunctionNamespaces.get(str)) != null) {
            return extensionHandler.isFunctionAvailable(str2);
        }
        return false;
    }

    public boolean elementAvailable(String str, String str2) throws TransformerException {
        ExtensionHandler extensionHandler;
        if (str != null && (extensionHandler = (ExtensionHandler) this.m_extensionFunctionNamespaces.get(str)) != null) {
            return extensionHandler.isElementAvailable(str2);
        }
        return false;
    }

    public Object extFunction(String str, String str2, Vector vector, Object obj, ExpressionContext expressionContext) throws TransformerException {
        if (str != null) {
            ExtensionHandler extensionHandler = (ExtensionHandler) this.m_extensionFunctionNamespaces.get(str);
            if (extensionHandler != null) {
                try {
                    return extensionHandler.callFunction(str2, vector, obj, expressionContext);
                } catch (TransformerException e) {
                    throw e;
                } catch (Exception e2) {
                    throw new TransformerException(e2);
                }
            }
            throw new XPathProcessorException(XSLMessages.createMessage(XSLTErrorResources.ER_EXTENSION_FUNC_UNKNOWN, new Object[]{str, str2}));
        }
        return null;
    }

    public Object extFunction(FuncExtFunction funcExtFunction, Vector vector, ExpressionContext expressionContext) throws TransformerException {
        String namespace = funcExtFunction.getNamespace();
        if (namespace != null) {
            ExtensionHandler extensionHandler = (ExtensionHandler) this.m_extensionFunctionNamespaces.get(namespace);
            if (extensionHandler != null) {
                try {
                    return extensionHandler.callFunction(funcExtFunction, vector, expressionContext);
                } catch (TransformerException e) {
                    throw e;
                } catch (Exception e2) {
                    throw new TransformerException(e2);
                }
            }
            throw new XPathProcessorException(XSLMessages.createMessage(XSLTErrorResources.ER_EXTENSION_FUNC_UNKNOWN, new Object[]{namespace, funcExtFunction.getFunctionName()}));
        }
        return null;
    }
}
