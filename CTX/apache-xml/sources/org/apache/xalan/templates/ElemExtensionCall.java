package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.extensions.ExtensionHandler;
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
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        this.m_extns = getNamespace();
        this.m_decl = getElemExtensionDecl(stylesheetRoot, this.m_extns);
        if (this.m_decl == null) {
            stylesheetRoot.getExtensionNamespacesManager().registerExtension(this.m_extns);
        }
    }

    private ElemExtensionDecl getElemExtensionDecl(StylesheetRoot stylesheetRoot, String str) {
        int globalImportCount = stylesheetRoot.getGlobalImportCount();
        for (int i = 0; i < globalImportCount; i++) {
            for (ElemTemplateElement firstChildElem = stylesheetRoot.getGlobalImport(i).getFirstChildElem(); firstChildElem != null; firstChildElem = firstChildElem.getNextSiblingElem()) {
                if (85 == firstChildElem.getXSLToken()) {
                    ElemExtensionDecl elemExtensionDecl = (ElemExtensionDecl) firstChildElem;
                    if (str.equals(firstChildElem.getNamespaceForPrefix(elemExtensionDecl.getPrefix()))) {
                        return elemExtensionDecl;
                    }
                }
            }
        }
        return null;
    }

    private void executeFallbacks(TransformerImpl transformerImpl) throws TransformerException {
        for (ElemTemplateElement elemTemplateElement = this.m_firstChild; elemTemplateElement != null; elemTemplateElement = elemTemplateElement.m_nextSibling) {
            if (elemTemplateElement.getXSLToken() == 57) {
                try {
                    transformerImpl.pushElemTemplateElement(elemTemplateElement);
                    ((ElemFallback) elemTemplateElement).executeFallback(transformerImpl);
                } finally {
                    transformerImpl.popElemTemplateElement();
                }
            }
        }
    }

    private boolean hasFallbackChildren() {
        for (ElemTemplateElement elemTemplateElement = this.m_firstChild; elemTemplateElement != null; elemTemplateElement = elemTemplateElement.m_nextSibling) {
            if (elemTemplateElement.getXSLToken() == 57) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        if (transformerImpl.getStylesheet().isSecureProcessing()) {
            throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_EXTENSION_ELEMENT_NOT_ALLOWED_IN_SECURE_PROCESSING, new Object[]{getRawName()}));
        }
        try {
            transformerImpl.getResultTreeHandler().flushPending();
            ExtensionHandler extensionHandler = transformerImpl.getExtensionsTable().get(this.m_extns);
            if (extensionHandler == null) {
                if (hasFallbackChildren()) {
                    executeFallbacks(transformerImpl);
                    return;
                } else {
                    transformerImpl.getErrorListener().fatalError(new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_CALL_TO_EXT_FAILED, new Object[]{getNodeName()})));
                    return;
                }
            }
            try {
                extensionHandler.processElement(getLocalName(), this, transformerImpl, getStylesheet(), this);
            } catch (Exception e) {
                if (hasFallbackChildren()) {
                    executeFallbacks(transformerImpl);
                    return;
                }
                if (e instanceof TransformerException) {
                    TransformerException transformerException = (TransformerException) e;
                    if (transformerException.getLocator() == null) {
                        transformerException.setLocator(this);
                    }
                    transformerImpl.getErrorListener().fatalError(transformerException);
                    return;
                }
                if (e instanceof RuntimeException) {
                    transformerImpl.getErrorListener().fatalError(new TransformerException(e));
                } else {
                    transformerImpl.getErrorListener().warning(new TransformerException(e));
                }
            }
        } catch (TransformerException e2) {
            transformerImpl.getErrorListener().fatalError(e2);
        } catch (SAXException e3) {
            throw new TransformerException(e3);
        }
    }

    public String getAttribute(String str, Node node, TransformerImpl transformerImpl) throws TransformerException {
        AVT literalResultAttribute = getLiteralResultAttribute(str);
        if (literalResultAttribute != null && literalResultAttribute.getRawName().equals(str)) {
            XPathContext xPathContext = transformerImpl.getXPathContext();
            return literalResultAttribute.evaluate(xPathContext, xPathContext.getDTMHandleFromNode(node), this);
        }
        return null;
    }

    @Override
    protected boolean accept(XSLTVisitor xSLTVisitor) {
        return xSLTVisitor.visitExtensionElement(this);
    }
}
