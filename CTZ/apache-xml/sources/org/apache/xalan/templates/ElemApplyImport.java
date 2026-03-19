package org.apache.xalan.templates;

import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;

public class ElemApplyImport extends ElemTemplateElement {
    static final long serialVersionUID = 3764728663373024038L;

    @Override
    public int getXSLToken() {
        return 72;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_APPLY_IMPORTS_STRING;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws Throwable {
        if (transformerImpl.currentTemplateRuleIsNull()) {
            transformerImpl.getMsgMgr().error(this, XSLTErrorResources.ER_NO_APPLY_IMPORT_IN_FOR_EACH);
        }
        int currentNode = transformerImpl.getXPathContext().getCurrentNode();
        if (-1 != currentNode) {
            transformerImpl.applyTemplateToNode(this, transformerImpl.getMatchedTemplate(), currentNode);
        } else {
            transformerImpl.getMsgMgr().error(this, XSLTErrorResources.ER_NULL_SOURCENODE_APPLYIMPORTS);
        }
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement elemTemplateElement) {
        error(XSLTErrorResources.ER_CANNOT_ADD, new Object[]{elemTemplateElement.getNodeName(), getNodeName()});
        return null;
    }
}
