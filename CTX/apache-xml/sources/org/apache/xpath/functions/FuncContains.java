package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

public class FuncContains extends Function2Args {
    static final long serialVersionUID = 5084753781887919723L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        String str = this.m_arg0.execute(xPathContext).str();
        String str2 = this.m_arg1.execute(xPathContext).str();
        if (str.length() == 0 && str2.length() == 0) {
            return XBoolean.S_TRUE;
        }
        return str.indexOf(str2) > -1 ? XBoolean.S_TRUE : XBoolean.S_FALSE;
    }
}
