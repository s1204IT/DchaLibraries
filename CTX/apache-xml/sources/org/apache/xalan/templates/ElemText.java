package org.apache.xalan.templates;

import org.apache.xalan.res.XSLTErrorResources;

public class ElemText extends ElemTemplateElement {
    static final long serialVersionUID = 1383140876182316711L;
    private boolean m_disableOutputEscaping = false;

    public void setDisableOutputEscaping(boolean z) {
        this.m_disableOutputEscaping = z;
    }

    public boolean getDisableOutputEscaping() {
        return this.m_disableOutputEscaping;
    }

    @Override
    public int getXSLToken() {
        return 42;
    }

    @Override
    public String getNodeName() {
        return "text";
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement elemTemplateElement) {
        if (elemTemplateElement.getXSLToken() != 78) {
            error(XSLTErrorResources.ER_CANNOT_ADD, new Object[]{elemTemplateElement.getNodeName(), getNodeName()});
        }
        return super.appendChild(elemTemplateElement);
    }
}
