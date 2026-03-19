package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.TransformerImpl;

public class ElemParam extends ElemVariable {
    static final long serialVersionUID = -1131781475589006431L;
    int m_qnameID;

    public ElemParam() {
    }

    @Override
    public int getXSLToken() {
        return 41;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_PARAMVARIABLE_STRING;
    }

    public ElemParam(ElemParam elemParam) throws TransformerException {
        super(elemParam);
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        this.m_qnameID = stylesheetRoot.getComposeState().getQNameID(this.m_qname);
        int xSLToken = this.m_parentNode.getXSLToken();
        if (xSLToken == 19 || xSLToken == 88) {
            ((ElemTemplate) this.m_parentNode).m_inArgsSize++;
        }
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        if (!transformerImpl.getXPathContext().getVarStack().isLocalSet(this.m_index)) {
            transformerImpl.getXPathContext().getVarStack().setLocalVariable(this.m_index, getValue(transformerImpl, transformerImpl.getXPathContext().getCurrentNode()));
        }
    }
}
