package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

public class FuncStartsWith extends Function2Args {
    static final long serialVersionUID = 2194585774699567928L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        return this.m_arg0.execute(xPathContext).xstr().startsWith(this.m_arg1.execute(xPathContext).xstr()) ? XBoolean.S_TRUE : XBoolean.S_FALSE;
    }
}
