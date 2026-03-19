package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.TransformerImpl;

public class ElemUnknown extends ElemLiteralResult {
    static final long serialVersionUID = -4573981712648730168L;

    @Override
    public int getXSLToken() {
        return -1;
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
        try {
            if (hasFallbackChildren()) {
                executeFallbacks(transformerImpl);
            }
        } catch (TransformerException e) {
            transformerImpl.getErrorListener().fatalError(e);
        }
    }
}
