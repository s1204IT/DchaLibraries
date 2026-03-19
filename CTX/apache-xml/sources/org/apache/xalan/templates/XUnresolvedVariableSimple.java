package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;

public class XUnresolvedVariableSimple extends XObject {
    static final long serialVersionUID = -1224413807443958985L;

    public XUnresolvedVariableSimple(ElemVariable elemVariable) {
        super(elemVariable);
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        XObject xObjectExecute = ((ElemVariable) this.m_obj).getSelect().getExpression().execute(xPathContext);
        xObjectExecute.allowDetachToRelease(false);
        return xObjectExecute;
    }

    @Override
    public int getType() {
        return XObject.CLASS_UNRESOLVEDVARIABLE;
    }

    @Override
    public String getTypeString() {
        return "XUnresolvedVariableSimple (" + object().getClass().getName() + ")";
    }
}
