package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.XPathContext;

public class ElemChoose extends ElemTemplateElement {
    static final long serialVersionUID = -3070117361903102033L;

    @Override
    public int getXSLToken() {
        return 37;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_CHOOSE_STRING;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        boolean z = false;
        for (ElemTemplateElement firstChildElem = getFirstChildElem(); firstChildElem != null; firstChildElem = firstChildElem.getNextSiblingElem()) {
            int xSLToken = firstChildElem.getXSLToken();
            if (38 == xSLToken) {
                ElemWhen elemWhen = (ElemWhen) firstChildElem;
                XPathContext xPathContext = transformerImpl.getXPathContext();
                if (!elemWhen.getTest().bool(xPathContext, xPathContext.getCurrentNode(), elemWhen)) {
                    z = true;
                } else {
                    transformerImpl.executeChildTemplates((ElemTemplateElement) elemWhen, true);
                    return;
                }
            } else if (39 == xSLToken) {
                transformerImpl.executeChildTemplates(firstChildElem, true);
                return;
            }
        }
        if (!z) {
            transformerImpl.getMsgMgr().error(this, XSLTErrorResources.ER_CHOOSE_REQUIRES_WHEN);
        }
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement elemTemplateElement) {
        switch (elemTemplateElement.getXSLToken()) {
            case 38:
            case 39:
                break;
            default:
                error(XSLTErrorResources.ER_CANNOT_ADD, new Object[]{elemTemplateElement.getNodeName(), getNodeName()});
                break;
        }
        return super.appendChild(elemTemplateElement);
    }

    @Override
    public boolean canAcceptVariables() {
        return false;
    }
}
