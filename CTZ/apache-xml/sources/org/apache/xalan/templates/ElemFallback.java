package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.TransformerImpl;

public class ElemFallback extends ElemTemplateElement {
    static final long serialVersionUID = 1782962139867340703L;

    @Override
    public int getXSLToken() {
        return 57;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_FALLBACK_STRING;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
    }

    public void executeFallback(TransformerImpl transformerImpl) throws TransformerException {
        int xSLToken = this.m_parentNode.getXSLToken();
        if (79 == xSLToken || -1 == xSLToken) {
            transformerImpl.executeChildTemplates((ElemTemplateElement) this, true);
        } else {
            System.out.println("Error!  parent of xsl:fallback must be an extension or unknown element!");
        }
    }
}
