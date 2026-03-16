package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.VariableStack;
import org.apache.xpath.objects.XObject;

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

    public ElemParam(ElemParam param) throws TransformerException {
        super(param);
    }

    @Override
    public void compose(StylesheetRoot sroot) throws TransformerException {
        super.compose(sroot);
        this.m_qnameID = sroot.getComposeState().getQNameID(this.m_qname);
        int parentToken = this.m_parentNode.getXSLToken();
        if (parentToken == 19 || parentToken == 88) {
            ((ElemTemplate) this.m_parentNode).m_inArgsSize++;
        }
    }

    @Override
    public void execute(TransformerImpl transformer) throws TransformerException {
        VariableStack vars = transformer.getXPathContext().getVarStack();
        if (!vars.isLocalSet(this.m_index)) {
            int sourceNode = transformer.getXPathContext().getCurrentNode();
            XObject var = getValue(transformer, sourceNode);
            transformer.getXPathContext().getVarStack().setLocalVariable(this.m_index, var);
        }
    }
}
