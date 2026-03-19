package org.apache.xpath.operations;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

public class Equals extends Operation {
    static final long serialVersionUID = -2658315633903426134L;

    @Override
    public XObject operate(XObject xObject, XObject xObject2) throws TransformerException {
        return xObject.equals(xObject2) ? XBoolean.S_TRUE : XBoolean.S_FALSE;
    }

    @Override
    public boolean bool(XPathContext xPathContext) throws TransformerException {
        XObject xObjectExecute = this.m_left.execute(xPathContext, true);
        XObject xObjectExecute2 = this.m_right.execute(xPathContext, true);
        boolean zEquals = xObjectExecute.equals(xObjectExecute2);
        xObjectExecute.detach();
        xObjectExecute2.detach();
        return zEquals;
    }
}
