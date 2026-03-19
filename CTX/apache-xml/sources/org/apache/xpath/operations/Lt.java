package org.apache.xpath.operations;

import javax.xml.transform.TransformerException;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

public class Lt extends Operation {
    static final long serialVersionUID = 3388420509289359422L;

    @Override
    public XObject operate(XObject xObject, XObject xObject2) throws TransformerException {
        return xObject.lessThan(xObject2) ? XBoolean.S_TRUE : XBoolean.S_FALSE;
    }
}
