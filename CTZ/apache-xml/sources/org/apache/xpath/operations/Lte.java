package org.apache.xpath.operations;

import javax.xml.transform.TransformerException;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

public class Lte extends Operation {
    static final long serialVersionUID = 6945650810527140228L;

    @Override
    public XObject operate(XObject xObject, XObject xObject2) throws TransformerException {
        return xObject.lessThanOrEqual(xObject2) ? XBoolean.S_TRUE : XBoolean.S_FALSE;
    }
}
