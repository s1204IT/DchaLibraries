package org.apache.xpath.operations;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

public class Or extends Operation {
    static final long serialVersionUID = -644107191353853079L;

    @Override
    public XObject execute(XPathContext xctxt) throws TransformerException {
        XObject expr1 = this.m_left.execute(xctxt);
        if (!expr1.bool()) {
            XObject expr2 = this.m_right.execute(xctxt);
            return expr2.bool() ? XBoolean.S_TRUE : XBoolean.S_FALSE;
        }
        return XBoolean.S_TRUE;
    }

    @Override
    public boolean bool(XPathContext xctxt) throws TransformerException {
        return this.m_left.bool(xctxt) || this.m_right.bool(xctxt);
    }
}
