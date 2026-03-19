package org.apache.xpath.operations;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

public class Bool extends UnaryOperation {
    static final long serialVersionUID = 44705375321914635L;

    @Override
    public XObject operate(XObject xObject) throws TransformerException {
        if (1 == xObject.getType()) {
            return xObject;
        }
        return xObject.bool() ? XBoolean.S_TRUE : XBoolean.S_FALSE;
    }

    @Override
    public boolean bool(XPathContext xPathContext) throws TransformerException {
        return this.m_right.bool(xPathContext);
    }
}
