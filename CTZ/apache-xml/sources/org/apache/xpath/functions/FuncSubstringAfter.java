package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

public class FuncSubstringAfter extends Function2Args {
    static final long serialVersionUID = -8119731889862512194L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        XMLString xMLStringXstr = this.m_arg0.execute(xPathContext).xstr();
        XMLString xMLStringXstr2 = this.m_arg1.execute(xPathContext).xstr();
        int iIndexOf = xMLStringXstr.indexOf(xMLStringXstr2);
        if (-1 == iIndexOf) {
            return XString.EMPTYSTRING;
        }
        return (XString) xMLStringXstr.substring(iIndexOf + xMLStringXstr2.length());
    }
}
