package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.extensions.ExtensionHandler;
import org.apache.xalan.extensions.ExtensionsTable;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.XPathContext;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ElemExtensionCall extends ElemLiteralResult {
    static final long serialVersionUID = 3171339708500216920L;
    ElemExtensionDecl m_decl = null;
    String m_extns;
    String m_lang;
    String m_scriptSrc;
    String m_srcURL;

    @Override
    public int getXSLToken() {
        return 79;
    }

    @Override
    public void compose(StylesheetRoot sroot) throws TransformerException {
        super.compose(sroot);
        this.m_extns = getNamespace();
        this.m_decl = getElemExtensionDecl(sroot, this.m_extns);
        if (this.m_decl == null) {
            sroot.getExtensionNamespacesManager().registerExtension(this.m_extns);
        }
    }

    private ElemExtensionDecl getElemExtensionDecl(StylesheetRoot stylesheet, String namespace) {
        int n = stylesheet.getGlobalImportCount();
        for (int i = 0; i < n; i++) {
            Stylesheet imported = stylesheet.getGlobalImport(i);
            for (ElemTemplateElement child = imported.getFirstChildElem(); child != null; child = child.getNextSiblingElem()) {
                if (85 == child.getXSLToken()) {
                    ElemExtensionDecl decl = (ElemExtensionDecl) child;
                    String prefix = decl.getPrefix();
                    String declNamespace = child.getNamespaceForPrefix(prefix);
                    if (namespace.equals(declNamespace)) {
                        return decl;
                    }
                }
            }
        }
        return null;
    }

    private void executeFallbacks(TransformerImpl transformer) throws TransformerException {
        for (ElemTemplateElement child = this.m_firstChild; child != null; child = child.m_nextSibling) {
            if (child.getXSLToken() == 57) {
                try {
                    transformer.pushElemTemplateElement(child);
                    ((ElemFallback) child).executeFallback(transformer);
                } finally {
                    transformer.popElemTemplateElement();
                }
            }
        }
    }

    private boolean hasFallbackChildren() {
        for (ElemTemplateElement child = this.m_firstChild; child != null; child = child.m_nextSibling) {
            if (child.getXSLToken() == 57) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void execute(TransformerImpl transformer) throws TransformerException {
        if (transformer.getStylesheet().isSecureProcessing()) {
            throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_EXTENSION_ELEMENT_NOT_ALLOWED_IN_SECURE_PROCESSING, new Object[]{getRawName()}));
        }
        try {
            transformer.getResultTreeHandler().flushPending();
            ExtensionsTable etable = transformer.getExtensionsTable();
            ExtensionHandler nsh = etable.get(this.m_extns);
            if (nsh == null) {
                if (hasFallbackChildren()) {
                    executeFallbacks(transformer);
                    return;
                } else {
                    transformer.getErrorListener().fatalError(new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_CALL_TO_EXT_FAILED, new Object[]{getNodeName()})));
                    return;
                }
            }
            try {
                nsh.processElement(getLocalName(), this, transformer, getStylesheet(), this);
            } catch (Exception e) {
                if (hasFallbackChildren()) {
                    executeFallbacks(transformer);
                    return;
                }
                if (e instanceof TransformerException) {
                    TransformerException te = (TransformerException) e;
                    if (te.getLocator() == null) {
                        te.setLocator(this);
                    }
                    transformer.getErrorListener().fatalError(te);
                    return;
                }
                if (e instanceof RuntimeException) {
                    transformer.getErrorListener().fatalError(new TransformerException(e));
                } else {
                    transformer.getErrorListener().warning(new TransformerException(e));
                }
            }
        } catch (TransformerException e2) {
            transformer.getErrorListener().fatalError(e2);
        } catch (SAXException se) {
            throw new TransformerException(se);
        }
    }

    public String getAttribute(String rawName, Node sourceNode, TransformerImpl transformer) throws TransformerException {
        AVT avt = getLiteralResultAttribute(rawName);
        if (avt == null || !avt.getRawName().equals(rawName)) {
            return null;
        }
        XPathContext xctxt = transformer.getXPathContext();
        return avt.evaluate(xctxt, xctxt.getDTMHandleFromNode(sourceNode), this);
    }

    @Override
    protected boolean accept(XSLTVisitor visitor) {
        return visitor.visitExtensionElement(this);
    }
}
