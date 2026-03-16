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
    public void execute(TransformerImpl transformer) throws TransformerException {
        boolean found = false;
        for (ElemTemplateElement childElem = getFirstChildElem(); childElem != null; childElem = childElem.getNextSiblingElem()) {
            int type = childElem.getXSLToken();
            if (38 == type) {
                found = true;
                ElemWhen when = (ElemWhen) childElem;
                XPathContext xctxt = transformer.getXPathContext();
                int sourceNode = xctxt.getCurrentNode();
                if (when.getTest().bool(xctxt, sourceNode, when)) {
                    transformer.executeChildTemplates((ElemTemplateElement) when, true);
                    return;
                }
            } else if (39 == type) {
                transformer.executeChildTemplates(childElem, true);
                return;
            }
        }
        if (!found) {
            transformer.getMsgMgr().error(this, XSLTErrorResources.ER_CHOOSE_REQUIRES_WHEN);
        }
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement newChild) {
        int type = newChild.getXSLToken();
        switch (type) {
            case 38:
            case 39:
                break;
            default:
                error(XSLTErrorResources.ER_CANNOT_ADD, new Object[]{newChild.getNodeName(), getNodeName()});
                break;
        }
        return super.appendChild(newChild);
    }

    @Override
    public boolean canAcceptVariables() {
        return false;
    }
}
