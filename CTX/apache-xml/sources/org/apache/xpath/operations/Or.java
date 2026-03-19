package org.apache.xpath.operations;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

public class Or extends Operation {
    static final long serialVersionUID = -644107191353853079L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        if (this.m_left.execute(xPathContext).bool()) {
            return XBoolean.S_TRUE;
        }
        return this.m_right.execute(xPathContext).bool() ? XBoolean.S_TRUE : XBoolean.S_FALSE;
    }

    @Override
    public boolean bool(XPathContext xPathContext) throws TransformerException {
        return this.m_left.bool(xPathContext) || this.m_right.bool(xPathContext);
    }
}
