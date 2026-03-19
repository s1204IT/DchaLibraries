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
    public void setPrefix(String v) {
        this.m_prefix = v;
    }

    @Override
    public String getPrefix() {
        return this.m_prefix;
    }

    public void setFunctions(StringVector v) {
        this.m_functions = v;
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

    public void setElements(StringVector v) {
        this.m_elements = v;
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
    public void compose(StylesheetRoot sroot) throws TransformerException {
        super.compose(sroot);
        String prefix = getPrefix();
        String declNamespace = getNamespaceForPrefix(prefix);
        String lang = null;
        String srcURL = null;
        String scriptSrc = null;
        if (declNamespace == null) {
            throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_NO_NAMESPACE_DECL, new Object[]{prefix}));
        }
        for (ElemTemplateElement child = getFirstChildElem(); child != null; child = child.getNextSiblingElem()) {
            if (86 == child.getXSLToken()) {
                ElemExtensionScript sdecl = (ElemExtensionScript) child;
                lang = sdecl.getLang();
                srcURL = sdecl.getSrc();
                ElemTemplateElement childOfSDecl = sdecl.getFirstChildElem();
                if (childOfSDecl != null && 78 == childOfSDecl.getXSLToken()) {
                    ElemTextLiteral tl = (ElemTextLiteral) childOfSDecl;
                    char[] chars = tl.getChars();
                    scriptSrc = new String(chars);
                    if (scriptSrc.trim().length() == 0) {
                        scriptSrc = null;
                    }
                }
            }
        }
        if (lang == null) {
            lang = "javaclass";
        }
        if (lang.equals("javaclass") && scriptSrc != null) {
            throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_ELEM_CONTENT_NOT_ALLOWED, new Object[]{scriptSrc}));
        }
        ExtensionNamespaceSupport extNsSpt = null;
        ExtensionNamespacesManager extNsMgr = sroot.getExtensionNamespacesManager();
        if (extNsMgr.namespaceIndex(declNamespace, extNsMgr.getExtensions()) == -1) {
            if (lang.equals("javaclass")) {
                if (srcURL == null) {
                    extNsSpt = extNsMgr.defineJavaNamespace(declNamespace);
                } else if (extNsMgr.namespaceIndex(srcURL, extNsMgr.getExtensions()) == -1) {
                    extNsSpt = extNsMgr.defineJavaNamespace(declNamespace, srcURL);
                }
            } else {
                Object[] args = {declNamespace, this.m_elements, this.m_functions, lang, srcURL, scriptSrc, getSystemId()};
                extNsSpt = new ExtensionNamespaceSupport(declNamespace, "org.apache.xalan.extensions.ExtensionHandlerGeneral", args);
            }
        }
        if (extNsSpt == null) {
            return;
        }
        extNsMgr.registerExtension(extNsSpt);
    }

    @Override
    public void runtimeInit(TransformerImpl transformer) throws TransformerException {
    }
}
