package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.extensions.ExtensionNamespaceSupport;
import org.apache.xalan.extensions.ExtensionNamespacesManager;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.utils.StringVector;

public class ElemExtensionDecl extends ElemTemplateElement {
    static final long serialVersionUID = -4692738885172766789L;
    private String m_prefix = null;
    private StringVector m_functions = new StringVector();
    private StringVector m_elements = null;

    @Override
    public void setPrefix(String str) {
        this.m_prefix = str;
    }

    @Override
    public String getPrefix() {
        return this.m_prefix;
    }

    public void setFunctions(StringVector stringVector) {
        this.m_functions = stringVector;
    }

    public StringVector getFunctions() {
        return this.m_functions;
    }

    public String getFunction(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_functions == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return this.m_functions.elementAt(i);
    }

    public int getFunctionCount() {
        if (this.m_functions != null) {
            return this.m_functions.size();
        }
        return 0;
    }

    public void setElements(StringVector stringVector) {
        this.m_elements = stringVector;
    }

    public StringVector getElements() {
        return this.m_elements;
    }

    public String getElement(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_elements == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return this.m_elements.elementAt(i);
    }

    public int getElementCount() {
        if (this.m_elements != null) {
            return this.m_elements.size();
        }
        return 0;
    }

    @Override
    public int getXSLToken() {
        return 85;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        String prefix = getPrefix();
        String namespaceForPrefix = getNamespaceForPrefix(prefix);
        if (namespaceForPrefix == null) {
            throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_NO_NAMESPACE_DECL, new Object[]{prefix}));
        }
        ExtensionNamespaceSupport extensionNamespaceSupport = null;
        String str = null;
        String str2 = null;
        String str3 = null;
        for (ElemTemplateElement firstChildElem = getFirstChildElem(); firstChildElem != null; firstChildElem = firstChildElem.getNextSiblingElem()) {
            if (86 == firstChildElem.getXSLToken()) {
                ElemExtensionScript elemExtensionScript = (ElemExtensionScript) firstChildElem;
                String lang = elemExtensionScript.getLang();
                String src = elemExtensionScript.getSrc();
                ElemTemplateElement firstChildElem2 = elemExtensionScript.getFirstChildElem();
                if (firstChildElem2 != null && 78 == firstChildElem2.getXSLToken()) {
                    str2 = new String(((ElemTextLiteral) firstChildElem2).getChars());
                    if (str2.trim().length() == 0) {
                        str2 = null;
                    }
                }
                str = lang;
                str3 = src;
            }
        }
        if (str == null) {
            str = "javaclass";
        }
        if (str.equals("javaclass") && str2 != null) {
            throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_ELEM_CONTENT_NOT_ALLOWED, new Object[]{str2}));
        }
        ExtensionNamespacesManager extensionNamespacesManager = stylesheetRoot.getExtensionNamespacesManager();
        if (extensionNamespacesManager.namespaceIndex(namespaceForPrefix, extensionNamespacesManager.getExtensions()) == -1) {
            if (str.equals("javaclass")) {
                if (str3 == null) {
                    extensionNamespaceSupport = extensionNamespacesManager.defineJavaNamespace(namespaceForPrefix);
                } else if (extensionNamespacesManager.namespaceIndex(str3, extensionNamespacesManager.getExtensions()) == -1) {
                    extensionNamespaceSupport = extensionNamespacesManager.defineJavaNamespace(namespaceForPrefix, str3);
                }
            } else {
                extensionNamespaceSupport = new ExtensionNamespaceSupport(namespaceForPrefix, "org.apache.xalan.extensions.ExtensionHandlerGeneral", new Object[]{namespaceForPrefix, this.m_elements, this.m_functions, str, str3, str2, getSystemId()});
            }
        }
        if (extensionNamespaceSupport != null) {
            extensionNamespacesManager.registerExtension(extensionNamespaceSupport);
        }
    }

    @Override
    public void runtimeInit(TransformerImpl transformerImpl) throws TransformerException {
    }
}
